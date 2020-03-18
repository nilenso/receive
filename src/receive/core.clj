(ns receive.core
  (:require [ring.adapter.jetty :as jetty]
            [bidi.ring :refer (make-handler)]))

(defn ping [request]
  {:status 200
   :headers {"Content-Type" "text/plain"}
   :body "Hey you have reached here"})

(defn not-found [request]
  {:status 404
   :body "404"})

(def handler
  (make-handler ["/"  [["ping" ping]
                       [true not-found]]]))

(defn -main [& args]
  (jetty/run-jetty handler {:port  3000
                            :join? false}))