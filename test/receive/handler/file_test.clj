(ns receive.handler.file-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [receive.handlers.file :as handler]
            [receive.service.files :as file-service]
            [receive.service.files-test :as files]
            [ring.mock.request :as mock]))

(def tempfile-name "tempfile.dat")
(def tempfolder-path "/tmp/")
(def tempfile-path (str tempfolder-path tempfile-name))

(defn tempfile->file [tempfile]
  {:tempfile tempfile
   :content-type "text/plain"
   :filename (.getName tempfile)})

(defn mock-upload-response [file]
  (handler/upload
   (-> (mock/request :post "/upload/")
       (assoc :content-type "multipart/form-data"
              :params {:file file}
              :multipart-params {:file file}
              :body (io/input-stream (:tempfile file))))))

(deftest upload-handler
  (with-redefs [handler/uuid-str
                (constantly "958a5425-060b-4aad-ba65-bf25e4458991")
                file-service/save-file
                (constantly "958a5425-060b-4aad-ba65-bf25e4458991")]
    (let [tempfile (files/create-temp-file tempfile-path)
          file (tempfile->file tempfile)
          mock-response (mock-upload-response file)]
      (is (= mock-response
             {:status 200
              :body {:name tempfile-name
                     :success true
                     :message "File saved successfully!"
                     :uid "958a5425-060b-4aad-ba65-bf25e4458991"}})))))

(deftest download-link-test
  (with-redefs [handler/uuid-str (constantly "958a5425-060b-4aad-ba65-bf25e4458991")
                file-service/get-absolute-filename (constantly "/tmp/tempfile.dat")]
    (let [uid (handler/uuid-str)
          mock-request (-> (mock/request :get (format "/download/api/%s/" uid))
                           (assoc :params {:id uid}))
          mock-response (handler/download-file mock-request)]
      (files/create-temp-file tempfile-path)
      (is (= (:status mock-response) 200))
      (is (-> mock-response :body (.exists))))))

(deftest download-ui-link-test
  (with-redefs [handler/uuid-str (constantly "958a5425-060b-4aad-ba65-bf25e4458991")
                file-service/get-filename (constantly "tempfile.dat")]
    (let [uid "958a5425-060b-4aad-ba65-bf25e4458991"
          mock-request (-> (mock/request :get (format "/download/%s/" uid))
                           (assoc :params {:id uid}))
          mock-response (handler/download-view mock-request)
          headers (:headers mock-response)
          status (:status mock-response)]
      (is (= headers {"Content-Type" "text/html"}))
      (is (= status 200)))))

(deftest download-ui-bad-link-test
  (with-redefs [file-service/get-filename (constantly nil)]
    (let [uid (handler/uuid-str)
          mock-request (mock/request :get (format "/download/%s/" uid))
          mock-response (handler/download-view mock-request)]
      (is (= mock-response
             {:status 302, :headers {"Location" "/404"}, :body ""})))))

(deftest share-handler
  (let [tempfile (files/create-temp-file "/tmp/tempfile.dat")
        file (tempfile->file tempfile)
        upload-response (mock-upload-response file)
        share-url (format "/share?uid=%s" (-> upload-response :body :uid))
        mock-request (mock/request :get share-url)]
    (is (= (:status (handler/share-handler mock-request)) 200))))

(deftest index-handler
  (let [mock-response (handler/index (mock/request :get "/"))
        mock-status (:status mock-response)]
    (is (= mock-status 200))))

(defn cleanup-tempfile [f]
  (f)
  (io/delete-file tempfile-path))

;; TODO: Add functionality to use fixtures per test
(use-fixtures :once cleanup-tempfile)
