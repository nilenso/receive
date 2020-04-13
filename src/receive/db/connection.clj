(ns receive.db.connection
  (:require [next.jdbc :as jdbc]
            [receive.config :as config]))

(def database (:db config/config))

(def datasource (jdbc/get-datasource database))