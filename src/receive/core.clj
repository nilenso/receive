(ns receive.core
  (:require [ring.adapter.jetty :as jetty]))

(defn handler [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body "Receive - Work in Progress"})

(defn -main [& args]
  (jetty/run-jetty handler {:port  3000
                      :join? false}))