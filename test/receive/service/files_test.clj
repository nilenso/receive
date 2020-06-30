(ns receive.service.files-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [clojure.java.io :as io]
   [receive.handlers.file :refer [uuid-str]]
   [next.jdbc.sql :refer [find-by-keys]]
   [receive.db.connection :refer [datasource]]
   [receive.model.file :as model]
   [receive.service.files :as files])
  (:import (java.util UUID)))

(def ^:dynamic *tempfile* nil)

(def file-data
  {:filename "xjU5T7SpfhdvbpB5AOnYo"
   :uid #uuid "93dd0920-b5d7-11ea-a650-4c32759dd39d"
   :dt_created #inst "2020-06-24T05:00:09.098531000-00:00"
   :dt_expire nil})

(defn tempfile-location [filename]
  (str "/tmp/" filename))

(defn get-file [uid]
  (find-by-keys datasource :file_storage {:uid (UUID/fromString uid)}))

(deftest get-filename-test
  (with-redefs [model/find-file
                (constantly (select-keys file-data  [:filename :uid]))]
    (testing "should return an existing file"
      (is (= (files/get-filename (str (:uid file-data)))
             (:filename file-data)))))
  (testing "should return error when file does not exists"
    (is (= (files/get-filename (uuid-str))
           {:error :not-found}))))

(deftest save-file-to-disk
  (testing "should save file to the specified location"
    (let [filename (tempfile-location
                    (:filename file-data))]
      (files/save-to-disk *tempfile* filename)
      (is (.exists (io/file filename))))))

(deftest find-file-bad-uid-test
  (is (= (files/get-filename (uuid-str))
         {:error :not-found})))

(def public-file-data
  {:filename "tempfile.dat"
   :owner-id nil
   :shared-with-users nil
   :is-private false
   :expired false})

(def private-file-data
  {:filename "tempfile.dat"
   :owner-id 1
   :shared-with-users [2 3]
   :is-private true
   :expired false})

(deftest has-read-access-test
  (testing "should return true if file is not private"
    (with-redefs [model/find-file
                  (constantly public-file-data)]
      (is (true?
           (files/has-read-access? {:user_id 10} "mock_uid")))))
  (testing "should return false when private and not owner and not shared with"
    (with-redefs [model/find-file
                  (constantly private-file-data)
                  files/is-shared-with?
                  (constantly false)]
      (is (false?
           (files/has-read-access? {:user_id 10} "mock_uid")))))
  (testing "should return true when private and owner"
    (with-redefs [model/find-file
                  (constantly private-file-data)]
      (is (true?
           (files/has-read-access? {:user_id 1} "mock_uid")))))
  (testing "should return true when not owner by shared with"
    (with-redefs [model/find-file
                  (constantly private-file-data)
                  files/is-shared-with?
                  (constantly true)]
      (is (true?
           (files/has-read-access? {:user_id 10} "mock_uid"))))))

(defn create-temp-file
  "Creates a file given a filename"
  [file]
  (with-open [file (io/writer file)]
    (.write file "Some data"))
  (io/file file))

(defn delete-tempfile [file]
  (io/delete-file file))

(defn file-fixture [f]
  (let [tempfile (create-temp-file
                  (tempfile-location
                   (:filename file-data)))]
    (binding [*tempfile* tempfile]
      (f))
    (delete-tempfile tempfile)))

(use-fixtures :each  file-fixture)

#_((def ^:dynamic *tempfile*
     (create-temp-file
      (str "/tmp/"
           (:file_storage/filename
            (create-file (factory/generate-file)))))))