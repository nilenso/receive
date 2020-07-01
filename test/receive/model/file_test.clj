(ns receive.model.file-test
  (:require
   [clj-time.coerce :as time-coerce]
   [clj-time.core :as time]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [next.jdbc.sql :refer [insert! delete! get-by-id update!]]
   [receive.db.connection :refer [datasource]]
   [receive.factory :as factory]
   [receive.model.file :as model])
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
  (insert! datasource :file_storage file-data))

(defn delete-file [{uid :uid}]
  (delete! datasource :file_storage {:uid uid}))

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

(defn file-fixture [f]
  (let [file-data (create-file (factory/generate-file))]
    (binding [*file-data* file-data]
      (f))
    (delete-file file-data)))

(use-fixtures :each file-fixture)

#_((def ^:dynamic *file-data*
     (create-file (factory/generate-file))))