(ns receive.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]))

(defonce config
  (let [env (or (System/getenv "ENV") "development")]
    (aero/read-config
     (io/resource "config.edn")
     {:profile (keyword env)})))

(defonce staging?
  (= (:env config) "staging"))

(defonce production?
  (= (:env config) "production"))
