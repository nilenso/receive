(ns receive.db.connection
  (:require [next.jdbc :as jdbc]
            [receive.config :as config]
            [receive.db.helper :refer [as-unqualified-kebab-maps]]))

(def database (:db config/config))

(def datasource
  (jdbc/with-options
    (jdbc/get-datasource database)
    {:builder-fn as-unqualified-kebab-maps}))