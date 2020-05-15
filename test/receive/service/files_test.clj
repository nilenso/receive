(ns receive.service.files-test
  (:require [clojure.test :refer [deftest is]]
            [receive.service.files :as files]
            [clojure.java.io :as io]
            [receive.core :refer [uuid-str]]))

(defn create-temp-file
  "Creates a file given a filename"
  [file]
  (with-open [file (io/writer file)]
    (.write file "Some data"))
  (io/file file))

(deftest find-file-test
  (let [uid (uuid-str)
        tempfile (create-temp-file "/tmp/tempfile.dat")
        filename (str uid "_tempfile.dat")]
    (files/save-file tempfile filename uid)
    (is (= (files/get-filename uid)
           filename))))

(deftest find-file-bad-uid-test
  (let [uid (uuid-str)
        tempfile (create-temp-file "/tmp/tempfile.dat")
        filename (str uid "_tempfile.dat")]
    (files/save-file tempfile filename uid)
    (is (nil? (files/get-filename (uuid-str))))))

(deftest save-file-to-disk
  (let [filename "/tmp/save-to-file-test"
        tempfile (create-temp-file "/tmp/test_tempfile.dat")]
    (files/save-to-disk tempfile filename)
    (is (.exists (io/file filename)))))