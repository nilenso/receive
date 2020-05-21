(ns receive.middlewares
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure.walk :refer [keywordize-keys]]
            [receive.auth.jwt :as jwt]))

(defn wrap-fallback-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e)
        {:status 500 :body {:success false
                            :message "Server error"}}))))

(defn wrap-postgres-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch org.postgresql.util.PSQLException e
        (log/error e)
        {:status 400
         :body {:success false
                :message "Invalid data"}}))))

(defn trim-trailing-slash [uri]
  (when (and (not= uri "/")
             (.endsWith uri "/"))
    (-> uri drop-last string/join)))

(defn wrap-with-uri-rewrite [handler f]
  (fn [{uri :uri :as request}]
    (if-let [rewrite (f uri)]
      (handler (assoc request :uri rewrite))
      (handler request))))

(defn verified-user [handler]
  (fn [request]
    (if-let [access-token (-> request
                              :cookies
                              :access_token
                              :value)]
      (handler (assoc request :auth (jwt/verify access-token)))
      (handler (assoc request :auth nil)))))

(defn wrap-cookies-keyword [handler]
  (fn [request]
    (handler (assoc request
                    :cookies
                    (keywordize-keys (:cookies request))))))