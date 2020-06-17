(ns receive.handlers.api
  (:require [receive.service.user :as user]
            [receive.service.files :as files]
            [receive.error-handler :refer [if-error]]))

(def ping (constantly
           {:status 200
            :body {:success true
                   :message "Server is running fine!"}}))

(def not-found
  (constantly {:status 404
               :body {:success false
                      :message "Not found"}}))

(defn sign-in [request]
  (let [id-token (-> request :params :id_token)
        token (user/signin-with-google id-token)]
    (if-error token
              :http-response
              {:status 200
               :cookies {"access_token" {:value token
                                 ;; TODO: set :secure true after HTTPS is enabled
                                         :http-only true
                                         :same-site :strict
                                         :path "/"}}
               :body {:data token
                      :success true
                      :message "User authenticated"}})))

(defn fetch-user [{auth :auth}]
  (if auth
    {:status 200
     :body {:success true
            :data
            (user/get-user (:user_id auth))}}
    {:status 401
     :body {:status false
            :message "Not authenticated"}}))

(defn update-file [{:keys [params route-params auth]}]
  (let [result (files/find-and-update-file auth
                                           (:id route-params)
                                           {:private? (:is_private params)
                                            :shared-with-user-emails (:shared_with_users params)})]
    (if-error result
              :http-response
              {:status 200
               :body {:success true
                      :data result}})))