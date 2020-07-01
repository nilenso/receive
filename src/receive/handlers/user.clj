(ns receive.handlers.user
  (:require
   [receive.error-handler :refer [error->http-response
                                  if-error]]
   [receive.handlers.helper :refer [success]]
   [receive.service.user :as user]))

(defn fetch-user [{auth :auth}]
  (if auth
    (success (user/get-user (:user_id auth)))
    (error->http-response {:error :unauthorized})))

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