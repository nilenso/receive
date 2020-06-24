(ns receive.handlers.api-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [receive.handlers.api :as handler]
   [receive.service.files :as file-service]
   [receive.model.file :as file-model]
   [receive.service.user :as user-service]
   [receive.handlers.file :refer [uuid-str]]
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

(def update-file-result
  {:filename "image1.png"
   :uid #uuid "94c22936-b0bb-11ea-9e05-4c32759dd39d"
   :dt_created #inst "2020-06-17T16:57:08.931927000-00:00"
   :dt_expire #inst "2020-06-19T16:57:08.930000000-00:00"
   :dt_updated #inst "2020-06-17T17:58:06.365726000-00:00"
   :is_private true
   :shared_with_users [11 12]
   :owner_id 10})

(def find-file-result
  {:file_storage/filename "image1.png"
   :file_storage/owner_id 10
   :file_storage/shared_with_users []
   :expired false})

(deftest update-file-test
  (with-redefs [user-service/find-or-create
                (constantly [{:id 11} {:id 12}])
                file-service/update-file-data
                (constantly update-file-result)
                file-service/find-file (constantly find-file-result)]
    (let [uid (str (:uid update-file-result))
          mock-request (-> (mock/request :put (str "/api/user/files/" uid))
                           (assoc :params {:is_private true
                                           :shared_with_users ["email1@google.com"
                                                               "email2@gmail.com"]}
                                  :route-params {:id uid}
                                  :auth {:user_id 10}))
          mock-response (handler/update-file mock-request)
          status (:status mock-response)
          is-private (-> mock-response :body :data :is_private)]
      (is (= 200 status))
      (is (true? is-private)))))

(def shared-with-user-details
  [{:id 69
    :first_name "email@something.com"
    :last_name nil
    :email "email@something.com"
    :dt_created #inst "2020-06-19T09:36:57.277049000-00:00"
    :dt_updated #inst "2020-06-19T09:36:57.277049000-00:00"
    :status "unregistered"}])

(deftest get-shared-with-users-test
  (with-redefs [file-service/get-shared-user-details
                (constantly shared-with-user-details)
                file-model/is-file-owner? (constantly true)]
    (let [uid (uuid-str)
          mock-response (-> (mock/request :get
                                          (str "/api/user/files/" uid "/shared"))
                            (assoc :route-params {:id uid}
                                   :auth {:user_id 11})
                            (handler/get-shared-with-users))]
      (is (= 200 (:status mock-response)))
      (is (= (-> mock-response :body :data)
             shared-with-user-details)))))

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
