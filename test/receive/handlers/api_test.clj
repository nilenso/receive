(ns receive.handlers.api-test
  (:require [clojure.test :refer [deftest is testing]]
            [receive.handlers.api :as handler]
            [receive.service.user :as user-service]
            [receive.service.files :as file-service]
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

(deftest sign-in-handler-failed
  (with-redefs [user-service/signin-with-google
                (constantly {:error :jwt-no-token})]
    (is (=
         (handler/sign-in (-> (mock/request :put "/user")
                              (assoc :params {:id_token "mock_token"})))
         {:status 400
          :body {:success false
                 :message "No token provided"}}))))

(def get-uploaded-files-data
  [#:file_storage{:id 194
                  :filename "saber.png"
                  :uid "3b24ceb1-42cd-459b-ba74-8a82dad5cbb6"
                  :created_at #inst "2020-06-09T09:13:34.396941000-00:00"
                  :user_id 111}
   #:file_storage{:id 195
                  :filename "saber.png"
                  :uid "33327486-9830-4c16-bbae-995d695195aa"
                  :created_at #inst "2020-06-09T09:13:58.564455000-00:00"
                  :user_id 111}])

(def uploaded-file-response
  {:status 200
   :body {:success true
          :data ({:filename "saber.png"
                  :uid "3b24ceb1-42cd-459b-ba74-8a82dad5cbb6", :created_at #inst "2020-06-09T09:13:34.396-00:00"}
                 {:filename "saber.png"
                  :uid "33327486-9830-4c16-bbae-995d695195aa"
                  :created_at #inst "2020-06-09T09:13:58.564-00:00"})}})

(deftest uploaded-files-test
  (testing "should return body for not authenticated when no auth provided"
    (with-redefs
     [file-service/get-uploaded-files
      (constantly get-uploaded-files-data)]
      (is (= (handler/uploaded-files
              (mock/request :get "/api/user/files"))
             {:status 401
              :body {:success false
                     :message "Not authenticated"}}))))
  (testing "should return list of uploaded files for user"
    (with-redefs
     [file-service/get-uploaded-files
      (constantly get-uploaded-files-data)]
      (let [response (handler/uploaded-files
                      (assoc
                       (mock/request :get "/api/user/files")
                       :auth {}))]
        (is (= 200 (:status  response)))
        (is (-> response :body :success))
        (is (= (-> response :body :data count) 2))))))