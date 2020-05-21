(ns receive.handlers.api-test
  (:require [clojure.test :refer [deftest is]]
            [receive.handlers.api :as handler]
            [receive.service.user :as user-service]
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

(deftest sign-in-handler
  (with-redefs [user-service/signin-with-google (constantly "jwt_token")]
    (is (=
         (handler/sign-in (-> (mock/request :put "/user")
                          (assoc :params {:id_token "mock_token"})))
         {:status 200
          :cookies {"access_token" {:value "jwt_token"
                                    :http-only true
                                    :same-site :strict
                                    :path "/"}}
          :body {:data "jwt_token", :success true, :message "User authenticated"}}))))