(ns receive.service.files-test
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [clojure.java.io :as io]
   [receive.handlers.file :refer [uuid-str]]
   [next.jdbc.sql :refer [insert! delete! find-by-keys]]
   [receive.db.connection :refer [datasource]]
   [receive.factory :as factory]
   [receive.service.files :as files])
  (:import (java.util UUID)))

(def ^:dynamic *tempfile* nil)
(def ^:dynamic *file-data* nil)

(defn tempfile-location [filename]
  (str "/tmp/" filename))

(defn get-file [uid]
  (find-by-keys datasource :file_storage {:uid (UUID/fromString uid)}))

(deftest get-filename-test
  (testing "should return an existing file"
    (is (= (files/get-filename (str (:file_storage/uid *file-data*)))
           (:file_storage/filename *file-data*))))
  (testing "should return error when file does not exists"
    (is (= (files/get-filename (uuid-str))
           {:error :not-found}))))

(deftest save-file-to-disk
  (testing "should save file to the specified location"
    (let [filename (tempfile-location
                    (:file_storage/filename *file-data*))]
      (files/save-to-disk *tempfile* filename)
      (is (.exists (io/file filename))))))

(deftest find-file-bad-uid-test
  (is (= (files/get-filename (uuid-str))
         {:error :not-found})))

(defn keywords->sql-keywords [data]
  (into {}
        (for [[k v] data]
          [(csk/->snake_case k) v])))

(defn create-temp-file
  "Creates a file given a filename"
  [file]
  (with-open [file (io/writer file)]
    (.write file "Some data"))
  (io/file file))

(defn delete-tempfile [file]
  (io/delete-file file))

(defn create-file [file-data]
  (insert! datasource :file_storage
           (keywords->sql-keywords file-data)))

(defn delete-file [{uid :file_storage/uid}]
  (delete! datasource :file_storage {:uid uid}))

(defn file-fixture [f]
  (let [file-data (create-file (factory/generate-file))
        tempfile (create-temp-file
                  (tempfile-location
                   (:file_storage/filename file-data)))]
    (binding [*tempfile* tempfile
              *file-data* file-data]
      (f))
    (delete-tempfile tempfile)
    (delete-file file-data)))

(use-fixtures :each  file-fixture)

#_((def ^:dynamic *tempfile*
     (create-temp-file
      (str "/tmp/"
           (:file_storage/filename
            (create-file (factory/generate-file))))))
   (def ^:dynamic *file-data*
     (create-file (factory/generate-file))))