(ns receive.fixtures
  (:require [next.jdbc :as jdbc]
            [receive.db.connection :refer [datasource]]
            [receive.factory :as factory]
            [next.jdbc.sql :refer [insert! delete!]]
            [camel-snake-kebab.core :as csk]))

(defn clear-all-db-tables []
  (clojure.tools.logging/info "Clearing all DB tables"))

(defn clear-state [f]
  (f)
  (clear-all-db-tables)
  #_(clear-all-files))