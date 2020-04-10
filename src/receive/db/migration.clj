(ns receive.db.migration
  (:require [ragtime.jdbc :as jdbc]
            [receive.config :refer[config]]
            [ragtime.repl :as repl]))

(def migration-config
  {:datastore  (jdbc/sql-database (:db config))
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (repl/migrate migration-config))

(defn rollback []
  (repl/rollback migration-config))