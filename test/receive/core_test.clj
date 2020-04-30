(ns receive.core-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [receive.core :as core]
            [receive.service.persistence-test :as persistence]
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
    (let [tempfile (persistence/create-temp-file "/tmp/tempfile.dat")
          file (tempfile->file tempfile)
          mock-response (mock-response file)]
      (is (= mock-response
             {:status 200
              :body {:name "tempfile.dat"
                     :success true
                     :message "File saved successfully!"
                     :uid "958a5425-060b-4aad-ba65-bf25e4458991"}})))))

(deftest share-handler
  (let [tempfile (persistence/create-temp-file "/tmp/tempfile.dat")
        file (tempfile->file tempfile)
        upload-response (mock-response file)
        share-url (format "/share?uid=%s" (-> upload-response :body :uid))
        mock-request (mock/request :get share-url)]
    (is (= (:status (core/share-handler mock-request)) 200))))

(deftest index-handler
  (let [mock-response (core/index (mock/request :get "/"))
        mock-status (:status mock-response)]
    (is (= mock-status 200))))