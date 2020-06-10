(ns receive.fixtures
  (:require
   [next.jdbc :as jdbc]
   [receive.db.connection :refer [datasource]]
   [receive.db.migration :as migration]))

(defn drop-all-user-tables []
  (jdbc/execute-one! datasource
                     ["DROP OWNED BY CURRENT_USER"]))

(defn clear-state [f]
  (drop-all-user-tables)
  (migration/migrate)
  (f))