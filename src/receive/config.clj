(ns receive.config
  (:require [aero.core :as aero]
            [clojure.java.io :as io]))

(defonce config (aero/read-config (io/resource "config.edn")))