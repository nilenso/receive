(ns receive.core-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [receive.core :as core]
            [receive.service.files-test :as files]
            [ring.mock.request :as mock]))

(deftest ping-handler
  (is (= (core/ping (mock/request :get "/ping"))
         {:status 200
          :body {:success true
                 :message "Server is running fine!"}})))

(deftest not-found-handler
  (is (= (core/not-found (mock/request :get "/bad_route"))
         {:status 404
          :body {:success false
                 :message "Not found"}})))

(defn tempfile->file [tempfile]
  {:tempfile tempfile
   :content-type "text/plain"
   :filename (.getName tempfile)})

(defn mock-response [file]
  (core/upload
   (-> (mock/request :post "/upload/")
       (assoc :content-type "multipart/form-data"
              :params {"file" file}
              :multipart-params {"file" file}
              :body (io/input-stream (:tempfile file))))))

(deftest upload-handler
  (with-redefs [core/uuid-str
                (constantly "958a5425-060b-4aad-ba65-bf25e4458991")]
    (let [tempfile (files/create-temp-file "/tmp/tempfile.dat")
          file (tempfile->file tempfile)
          mock-response (mock-response file)]
      (is (= mock-response
             {:status 200
              :body {:name "tempfile.dat"
                     :success true
                     :message "File saved successfully!"
                     :uid "958a5425-060b-4aad-ba65-bf25e4458991"}})))))

(deftest download-link-test
  (let [tempfile (files/create-temp-file "/tmp/tempfile.dat")
        file (tempfile->file tempfile)
        mock-upload-response (mock-response file)
        uid (-> mock-upload-response :body :uid)
        mock-request (-> (mock/request :get (format "/download/%s/" uid))
                         (assoc :params {:id uid}))
        mock-response (core/download-view mock-request)
        headers (:headers mock-response)
        status (:status mock-response)]
    (is (= headers {"Content-Type" "text/html"}))
    (is (= status 200))))

(deftest download-bad-link-test
  (let [uid (core/uuid-str)
        mock-request (mock/request :get (format "/download/%s/" uid))
        mock-response (core/download-view mock-request)]
    (is (= mock-response
           {:status 302, :headers {"Location" "/404"}, :body ""}))))

(deftest index-handler
  (let [mock-response (core/index (mock/request :get "/"))
        mock-status (:status mock-response)]
    (is (= mock-status 200))))