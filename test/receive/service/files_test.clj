(ns receive.service.files-test
  (:require
   [camel-snake-kebab.core :as csk]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [clojure.java.io :as io]
   [receive.handlers.file :refer [uuid-str]]
   [next.jdbc.sql :refer [insert! delete! find-by-keys]]
   [receive.db.connection :refer [datasource]]
   [receive.factory :as factory]
   [receive.service.files :as files]
   [receive.spec.file :as spec])
  (:import (java.util UUID)))

(def ^:dynamic *user-id* nil)
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

(deftest save-file-test
  (testing "should add user-id to db when save-file with auth"
    (let [filename (:file_storage/filename *file-data*)
          uid      (files/save-file *tempfile*
                                    {:filename filename
                                     :uid      (uuid-str)
                                     :user-id  *user-id*})
          [file]   (get-file uid)]
      (is (not= *user-id* nil))
      (is (= (:file_storage/user_id file)
             *user-id*)))))

(deftest save-file-no-auth-test
  (testing "should not add user-id when there's no auth"
    (let [filename (:file_storage/filename *file-data*)
          uid      (files/save-file *tempfile*
                                    {:filename filename
                                     :uid      (uuid-str)})
          [file]   (get-file uid)]
      (is (not= *user-id* nil))
      (is (= (:file_storage/user_id file)
             nil)))))

(deftest get-uploaded-files-test
  (testing "should return a list of files uploaded by a user"
    (let [uploaded-files (files/get-uploaded-files *user-id*)]
      (is (= (count uploaded-files) 1))
      (is (every? spec/valid-db-entry? uploaded-files)))))

(defn keywords->sql-keywords [data]
  (into {}
        (for [[k v] data]
          [(csk/->snake_case k) v])))

(defn create-user []
  (insert! datasource :users (keywords->sql-keywords
                              (factory/generate-user))))

(defn delete-user [{id :id}]
  (delete! datasource :users {:id id}))

(defn user-fixture [f]
  (let [user (create-user)]
    (binding [*user-id* (:users/id user)]
      (f))
    (delete-user user)))

(defn create-temp-file
  "Creates a file given a filename"
  [file]
  (with-open [file (io/writer file)]
    (.write file "Some data"))
  (io/file file))

(deftest find-file-test
  (let [filename (:file_storage/filename *file-data*)
        uid (files/save-file *tempfile* {:filename filename})]
    (is (= (files/get-filename uid)
           filename))))

(deftest find-file-bad-uid-test
  (is (= (files/get-filename (uuid-str))
         {:error :not-found})))

(defn delete-tempfile [file]
  (io/delete-file file))

(defn create-file [file-data]
  (insert! datasource :file_storage
           (keywords->sql-keywords
            (assoc file-data
                   :user_id *user-id*))))

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

(use-fixtures :each user-fixture file-fixture)

#_((def ^:dynamic *tempfile*
     (create-temp-file
      (str "/tmp/"
           (:file_storage/filename
            (create-file (factory/generate-file))))))
   (def ^:dynamic *file-data*
     (create-file (factory/generate-file)))
   (def ^:dynamic *user-id*
     (:users/id create-user)))