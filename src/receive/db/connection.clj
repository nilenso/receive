(ns receive.db.connection
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as rs]
   [receive.config :as config]
   [receive.db.helper :refer [as-unqualified-kebab-maps]])
  (:import [java.sql Array]))

(def database (:db config/config))

(def datasource
  (jdbc/with-options
    (jdbc/get-datasource database)
    {:builder-fn as-unqualified-kebab-maps}))

(extend-protocol rs/ReadableColumn
  Array
  (read-column-by-label [^Array v _]    (vec (.getArray v)))
  (read-column-by-index [^Array v _ _]  (vec (.getArray v))))