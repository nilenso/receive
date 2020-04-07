(ns receive.db.connection
  (:require [next.jdbc :as jdbc]
            [clojure.java.io :refer [resource]]
            [aero.core :refer [read-config]]))

(defonce config (read-config (resource "config.edn")))

(def database (:db config))

(def datasource (jdbc/get-datasource database))