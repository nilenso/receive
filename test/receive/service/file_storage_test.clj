(ns receive.service.file-storage-test
  (:require [clojure.test :refer [deftest is]]
            [receive.service.file-storage :as file-storage]
            [receive.service.persistence-test :refer [create-temp-file]]
   [receive.service.persistence :refer [process-uploaded-file]]
            [receive.core :refer [uuid-str]]))

(deftest find-file-test
  (let [uid (uuid-str)
        tempfile (create-temp-file "/tmp/tempfile.dat")
        filename (str uid "_tempfile.dat")]
    (process-uploaded-file tempfile filename uid)
    (is (= (file-storage/find-file uid) 
           filename))))

(deftest find-file-bad-uid-test
  (let [uid (uuid-str)
        tempfile (create-temp-file "/tmp/tempfile.dat")
        filename (str uid "_tempfile.dat")]
    (process-uploaded-file tempfile filename uid)
    (is (nil? (file-storage/find-file (uuid-str))))))

