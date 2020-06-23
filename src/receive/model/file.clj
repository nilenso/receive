(ns receive.model.file
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as result-set]
   [receive.db.connection :as connection]
   [receive.db.sql :as sql])
  (:import [java.util UUID]))

(defn find-file [uid]
  (jdbc/execute-one! connection/datasource
                     (sql/find-file (UUID/fromString uid))
                     {:return-keys true
                      :builder-fn result-set/as-unqualified-maps}))
