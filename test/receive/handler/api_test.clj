(ns receive.handler.api-test
  (:require [clojure.test :refer [deftest is]]
            [receive.handlers.api :as handler]
            [ring.mock.request :as mock]))

(deftest ping-handler
  (is (= (handler/ping (mock/request :get "/ping"))
         {:status 200
          :body {:success true
                 :message "Server is running fine!"}})))

(deftest not-found-handler
  (is (= (handler/not-found (mock/request :get "/bad_route"))
         {:status 404
          :body {:success false
                 :message "Not found"}})))