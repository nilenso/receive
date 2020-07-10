(ns receive.handlers.user-test
  (:require [clojure.test :refer [deftest is]]
            [receive.config :as config]
            [receive.error-handler :refer [error]]
            [receive.handlers.user :as user-handlers]
            [receive.service.user :as user-service]
            [ring.mock.request :as mock]))

(deftest sign-in-handler
  (with-redefs [user-service/signin-with-google (constantly "jwt_token")
                config/deployed? true]
    (is (=
         (user-handlers/sign-in (-> (mock/request :put "/user")
                                    (assoc :params {:id_token "mock_token"})))
         {:status 200
          :cookies {"access_token" {:value "jwt_token"
                                    :http-only true
                                    :secure true
                                    :same-site :strict
                                    :path "/"}}
          :body {:data "jwt_token", :success true, :message "User authenticated"}}))))

(deftest sign-in-handler-failed
  (with-redefs [user-service/signin-with-google
                (constantly (error :jwt-no-token))]
    (is (=
         (user-handlers/sign-in (-> (mock/request :put "/user")
                                    (assoc :params {:id_token "mock_token"})))
         {:status 400
          :body {:success false
                 :message "No token provided"}}))))