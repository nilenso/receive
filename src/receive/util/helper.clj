(ns receive.util.helper)

(defn uuid []
  (.toString (java.util.UUID/randomUUID)))