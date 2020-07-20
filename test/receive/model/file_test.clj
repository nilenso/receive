(ns receive.model.file-test
  (:require
   [clj-time.coerce :as time-coerce]
   [clj-time.core :as time]
   [clojure.java.io :as io]
   [clojure.test :refer [are deftest is testing use-fixtures]]
   [next.jdbc.sql :refer [insert! delete! get-by-id update!]]
   [receive.db.connection :refer [datasource]]
   [receive.factory :as factory]
   [receive.model.file :as model]
   [receive.spec.file :as spec])
  (:import java.util.UUID))

(def ^:dynamic *file-data* nil)

(defn get-file [uid]
  (get-by-id datasource :file_storage (UUID/fromString uid) :uid {}))

(defn expire-file [uid]
  (update! datasource :file_storage
           {:dt_expire (time-coerce/to-sql-time (time/now))}
           {:uid uid}))

(defn delete-tempfile [file]
  (io/delete-file file))

(defn create-file [file-data]
  (insert! datasource :file_storage
           (-> file-data
               (assoc :shared_with_users
                      (into-array Integer/TYPE (:shared-with-users file-data)))
               (select-keys [:filename :shared_with_users]))))

(defn delete-file [{uid :uid}]
  (delete! datasource :file_storage {:uid uid}))

(defn file-fixture [f]
  (let [file-data (create-file (factory/generate-file))]
    (binding [*file-data* file-data]
      (f))
    (delete-file file-data)))

(use-fixtures :each file-fixture)

(deftest get-file-test
  (testing "should be valid data entries"
    (is (true? (spec/valid-db-entry?
                (model/get-file
                 (-> *file-data* :uid str)))))))

(deftest find-file-test
  (testing "should return an existing file"
    (let [uid (-> *file-data* :uid str)
          file (model/find-file uid)]
      (is (= (:filename file)
             (:filename *file-data*)))))
  (testing "should return nil when file uid does not match"
    (is (nil? (model/find-file (str (UUID/randomUUID)))))))

(deftest save-file-test
  (testing "should save a file to disk"
    (let [file (model/save-file datasource nil "tempfile.dat" nil)
          uid (:uid file)]
      (is (= "tempfile.dat"
             (:filename (get-file (str uid)))))
      (delete-file file))))

(deftest find-expired-files-test
  (testing "should return list of expired files"
    (expire-file (:uid *file-data*))
    (let [files (model/find-expired-files)]
      (is (= (count files) 1))
      (is (= (:filename *file-data*)
             (:filename (get files 0)))))))

(deftest delete-db-entry-test
  (testing "should delete a file given uid"
    (let [result (model/delete-file datasource
                                    (:uid *file-data*))]
      (is (= 1 (:next.jdbc/update-count result))))))

(deftest update-file-data-uid-test
  (testing "should only update uid"
    (let [updated-file (model/update-file-data datasource
                                               (-> *file-data* :uid str)
                                               {:private? (not (:is-private *file-data*))})]
      (is (not= (:is-private *file-data*) (:is-private updated-file)))
      (are [key] (= (key *file-data*) (key updated-file))
        :filename
        :owner-id
        :uid
        :shared-with-users
        :dt-expire))))

(deftest update-file-data-shared-with-users-test
  (testing "should only update shared-with-users"
    (let [updated-file (model/update-file-data datasource
                                               (-> *file-data* :uid str)
                                               {:shared-with-user-ids [19]})]
      (is (not= (:shared-with-users *file-data*) (:shared-with-users updated-file)))
      (are [key] (= (key *file-data*) (key updated-file))
        :filename
        :owner-id
        :uid
        :is-private
        :dt-expire))))

(deftest update-file-data-expire-test
  (testing "should only update dt-expire"
    (let [updated-file (model/update-file-data datasource
                                               (-> *file-data* :uid str)
                                               {:dt-expire (time-coerce/to-sql-time (time/now))})]
      (is (not= (:dt-expire *file-data*) (:dt-expire updated-file)))
      (are [key] (= (key *file-data*) (key updated-file))
        :filename
        :owner-id
        :uid
        :is-private
        :shared-with-users))))

#_((def ^:dynamic *file-data*
     (create-file (factory/generate-file))))