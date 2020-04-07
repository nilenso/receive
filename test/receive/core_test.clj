(ns receive.core-test
  (:require [clojure.test :refer :all]
            [receive.core :as core]
            [receive.service.persistence :as persistence]
            [ring.mock.request :as mock]
            [clojure.java.io :as io]))

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

(defn create-temp-file [file]
  (with-open [file (io/writer file)]
    (.write file "Some data"))
  (io/file file))

(deftest save-file-to-disk
  (let [filename "/tmp/save-to-file-test"
        tempfile (create-temp-file "/tmp/test_tempfile.dat")]
    (persistence/save-to-disk tempfile filename)
    (is (.exists (io/file filename)))))

(deftest upload-handler
  (let [tempfile (create-temp-file "/tmp/tempfile.dat")
        file {:tempfile tempfile
              :content-type "text/plain"
              :filename "tempfile.dat"}
        mock-response (core/upload
                       (-> (mock/request :post "/upload/")
                           (assoc :content-type "multipart/form-data"
                                  :params {"file" file}
                                  :multipart-params {"file" file}
                                  :body (io/input-stream (:tempfile file)))))
        body (select-keys (:body mock-response) [:name])
        response (assoc {} :status (:status mock-response)
                        :body body)]
    (is (= response
           {:status 200
            :body {:name "tempfile.dat"}}))))