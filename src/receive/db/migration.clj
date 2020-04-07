(ns receive.db.migration
  (:require [ragtime.jdbc :as jdbc]
            [aero.core :refer [read-config]]
            [ragtime.repl :as repl]))

(def config (read-config (clojure.java.io/resource "config.edn")))

(def migration-config
  {:datastore  (jdbc/sql-database (:db config))
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate migration-config))

(defn rollback []
  (repl/rollback migration-config))