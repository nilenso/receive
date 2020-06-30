(ns receive.db.helper
  (:require
   [camel-snake-kebab.core :as csk]
   [next.jdbc.result-set :as result-set]))

(defn as-unqualified-kebab-maps [rs opts]
  (let [kebab (comp name csk/->kebab-case-keyword)]
    (result-set/as-unqualified-modified-maps
     rs (assoc opts
               :label-fn kebab))))