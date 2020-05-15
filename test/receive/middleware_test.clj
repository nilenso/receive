(ns receive.middleware-test
  (:require [clojure.test :refer [deftest is]]
            [receive.middlewares :refer [upload-validation]]
            [receive.handlers.file-test :refer [mock-upload-request
                                                tempfile->file]]
            [receive.service.files-test :refer [create-temp-file]]
            [receive.service.files :refer [save-file]]
            [receive.config :refer [config]]))

(deftest upload-validate-file-too-big
  (with-redefs
   [save-file (constantly "file.dat")]
    (let [file (-> "/tmp/tempfile.dat"
                   create-temp-file
                   tempfile->file
                   (assoc :size (inc (:max-file-size config))))
          mock-request (mock-upload-request file)]
      (is (= ((upload-validation identity) mock-request)
             {:status 413
              :body {:success false
                     :message "File too big"}})))))

(deftest upload-validate-file-exists
  (with-redefs
   [save-file (constantly "file.dat")]
    (let [file (-> "/tmp/tempfile.dat"
                   create-temp-file
                   tempfile->file
                   (assoc :size 0))
          mock-request (mock-upload-request file)]
      (is (= ((upload-validation identity) mock-request)
             {:status 400
              :body {:success false
                     :message "File not provided"}})))))

(defn rand-str [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 65))))))

(deftest upload-validate-filename
  (with-redefs
   [save-file (constantly "file.dat")]
    (let [filename (rand-str (inc (:max-filename-length config)))
          file (-> "/tmp/tempfile.dat"
                   create-temp-file
                   tempfile->file
                   (assoc :filename filename))
          mock-request (mock-upload-request file)]
      (is (= ((upload-validation identity) mock-request)
             {:status 400
              :body {:success false
                     :message "File name is too long"}})))))