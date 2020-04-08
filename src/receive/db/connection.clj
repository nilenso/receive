(ns receive.db.connection
  (:require [next.jdbc :as jdbc]
            [receive.util.config :refer [config]]))

(def database (:db config))

(def datasource (jdbc/get-datasource database))