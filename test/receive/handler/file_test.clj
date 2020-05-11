(ns receive.handler.file-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [receive.handlers.file :as handler]
            [receive.service.persistence-test :as persistence]
            [ring.mock.request :as mock]))

(deftest upload-handler
  (with-redefs [handler/uuid-str
                (constantly "958a5425-060b-4aad-ba65-bf25e4458991")]
    (let [tempfile (persistence/create-temp-file "/tmp/tempfile.dat")
          file {:tempfile tempfile
                :content-type "text/plain"
                :filename "tempfile.dat"}
          mock-response (handler/upload
                         (-> (mock/request :post "/upload/")
                             (assoc :content-type "multipart/form-data"
                                    :params {"file" file}
                                    :multipart-params {"file" file}
                                    :body (io/input-stream (:tempfile file)))))]
      (is (= mock-response
             {:status 200
              :body {:name "tempfile.dat"
                     :success true
                     :message "File saved successfully!"
                     :uid "958a5425-060b-4aad-ba65-bf25e4458991"}})))))

(deftest index-handler
  (let [mock-response (handler/index (mock/request :get "/"))
        mock-status (:status mock-response)]
    (is (= mock-status 200))))