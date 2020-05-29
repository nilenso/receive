(ns receive.service.files-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [receive.handlers.file :refer [uuid-str]]
            [receive.service.files :as files]))

(defn create-temp-file
  "Creates a file given a filename"
  [file]
  (with-open [file (io/writer file)]
    (.write file "Some data"))
  (io/file file))

(deftest find-file-test
  (let [temp-uid (uuid-str)
        tempfile (create-temp-file "/tmp/tempfile.dat")
        filename (str temp-uid "_tempfile.dat")
        uid (files/save-file tempfile filename)]
    (is (= (files/get-filename uid)
           filename))))

(deftest find-file-bad-uid-test
  (let [uid (uuid-str)
        tempfile (create-temp-file "/tmp/tempfile.dat")
        filename (str uid "_tempfile.dat")]
    (files/save-file tempfile filename)
    (is (= (files/get-filename (uuid-str))
           {:error :not-found}))))

(deftest save-file-to-disk
  (let [filename "/tmp/save-to-file-test"
        tempfile (create-temp-file "/tmp/test_tempfile.dat")]
    (files/save-to-disk tempfile filename)
    (is (.exists (io/file filename)))))