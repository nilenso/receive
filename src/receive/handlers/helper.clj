(ns receive.handlers.helper)

(defn map-response-data
  "Returns a fn that maps over data and returns un-qualified keywords
   Accepts a list of keywords, only the supplied keys will be returned
   in the hash-map"
  [& keys]
  (fn [data]
    (select-keys
     (into {}
           (for [[k v] data]
             [(-> k name keyword) v]))
     keys)))