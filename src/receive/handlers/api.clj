(ns receive.handlers.api
  (:require [receive.service.files :as files]
            [receive.service.user :as user]
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

(def not-auth-response
  {:status 401
   :body {:status false
          :message "Not authenticated"}})

(defn fetch-user [{auth :auth}]
  (if auth
    {:status 200
     :body {:success true
            :data
            (user/get-user (:user_id auth))}}
    not-auth-response))

(defn map-response-data
  "Returns a fn that maps over data and returns un-qualified keywords
   Accepts a list of keywords, only the supplied keys will be returned
   in the hash-map"
  [& keys]
  (fn [data]
    (select-keys
     (into {}
           (for [[k v] data]
             [(-> k name keyword) v]))
     keys)))

(defn uploaded-files [{auth :auth}]
  (if auth
    {:status 200
     :body {:success true
            :data (->> (:user_id auth)
                       (files/get-uploaded-files)
                       (map (map-response-data :filename
                                               :uid
                                               :created_at)))}}
    not-auth-response))