(ns receive.model.file-test
  (:require
   [clj-time.coerce :as time-coerce]
   [clj-time.core :as time]
   [clojure.java.io :as io]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [next.jdbc.sql :refer [insert! delete! get-by-id update!]]
   [receive.db.connection :refer [datasource]]
   [receive.factory :as factory]
   [receive.model.file :as model]
   [receive.util :as util])
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
           (util/keywords->sql-keywords file-data)))

(defn delete-file [{uid :file_storage/uid}]
  (delete! datasource :file_storage {:uid uid}))

(deftest find-file-test
  (testing "should return an existing file"
    (let [uid (-> *file-data* :file_storage/uid str)
          file (model/find-file uid)]
      (is (= (:filename file)
             (:file_storage/filename *file-data*)))))
  (testing "should return nil when file uid does not match"
    (is (nil? (model/find-file (str (UUID/randomUUID)))))))

(deftest find-expired-files-test
  (testing "should return list of expired files"
    (expire-file (:file_storage/uid *file-data*))
    (let [files (model/find-expired-files)]
      (is (= (count files) 1))
      (is (= (:file_storage/filename *file-data*)
             (:filename (get files 0)))))))

(deftest delete-db-entry-test
  (testing "should delete a file given uid"
    (let [result (model/delete-db-entry datasource
                                        (:file_storage/uid *file-data*))]
      (is (= 1 (:next.jdbc/update-count result))))))

(deftest save-file-test
  (testing "should save a file to disk"
    (let [file (model/save-file datasource "tempfile.dat" nil)
          uid (:file_storage/uid file)]
      (is (= "tempfile.dat"
             (:file_storage/filename (get-file (str uid)))))
      (delete-file file))))

(defn file-fixture [f]
  (let [file-data (create-file (factory/generate-file))]
    (binding [*file-data* file-data]
      (f))
    (delete-file file-data)))

(use-fixtures :each file-fixture)

#_((def ^:dynamic *file-data*
     (create-file (factory/generate-file))))