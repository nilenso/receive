(ns receive.db.migration
  (:require
   [clojure.tools.logging :as log]
   [ragtime.jdbc :as jdbc]
   [ragtime.repl :as repl]
   [receive.config :refer [config]]))

(def migration-config
  {:datastore  (jdbc/sql-database (:db config))
   :migrations (jdbc/load-resources "migrations")})

(defn migrate []
  (log/info "Migrating")
  (repl/migrate migration-config))

(defn rollback []
  (log/info "Rolling back")
  (repl/rollback migration-config))