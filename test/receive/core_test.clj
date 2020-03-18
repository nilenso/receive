(ns receive.core-test
  (:require [clojure.test :refer :all]
            [receive.core :as core]
            [ring.mock.request :as mock]))

(deftest ping-handler
  (is (= (core/ping (mock/request :get "/ping"))
         {:status 200
          :headers {"Content-Type" "text/plain"}
          :body "Hey you have reached here"})))