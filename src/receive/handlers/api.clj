(ns receive.handlers.api
  (:require [receive.service.user :as user]))

(def ping (constantly
           {:status 200
            :body {:success true
                   :message "Server is running fine!"}}))

(def not-found
  (constantly {:status 404
               :body {:success false
                      :message "Not found"}}))

(defn signup [request]
  (try
    (let [id-token (-> request :params :id_token)
          token (user/signin-with-google id-token)]
      {:status 200
       :cookies {"access_token" {:value token
                                 ;; TODO: set :secure true after HTTPS is enabled
                                 :http-only true
                                 :same-site :strict
                                 :path "/"}}
       :body {:data token
              :success true
              :message "User authenticated"}})
    (catch Exception e
      {:status 401
       :body {:success false
              :message (.getMessage e)}})))