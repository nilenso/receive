(ns receive.middlewares
  (:require [clojure.tools.logging :as log]))

(defn wrap-fallback-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error (.getMessage e))
        {:status 500 :body {:success false
                            :message "Server error"}}))))

(defn wrap-postgres-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch org.postgresql.util.PSQLException e
        (log/error (.getMessage e))
        {:status 400
         :body {:success false
                :message "Invalid data"}}))))