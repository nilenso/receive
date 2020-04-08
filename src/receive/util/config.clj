(ns receive.util.config
  (:require [clojure.java.io :refer [resource]]
            [aero.core :refer [read-config]]))

(defonce config (read-config (resource "config.edn")))