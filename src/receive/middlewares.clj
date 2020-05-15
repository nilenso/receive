(ns receive.middlewares
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]))

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