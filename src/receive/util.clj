(ns receive.util
  (:require
   [camel-snake-kebab.core :as csk]))

(defn keywords->sql-keywords [data]
  (into {}
        (for [[k v] data]
          [(csk/->snake_case k) v])))