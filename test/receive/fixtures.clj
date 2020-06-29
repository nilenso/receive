(ns receive.fixtures
  (:require
   [clojure.tools.logging :as log]
   [next.jdbc :as jdbc]
   [receive.db.connection :refer [datasource]]
   [receive.db.migration :as migration]))

(defn drop-all-user-tables []
  (let [env (System/getenv "ENV")]
    (when (= env "test")
      (log/info "Dropping tables owned by user")
      (jdbc/execute! datasource
                     ["DROP OWNED BY CURRENT_USER"]))))

(defn clear-state
  ([]
   (drop-all-user-tables)
   (migration/migrate))
  ([f]
   (clear-state)
   (f)))