(ns receive.core
  (:require
   [receive.config :refer [config]]
   [receive.routes :as routes]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.reload :refer [wrap-reload]]))

(defn start-server
  []
  (jetty/run-jetty routes/handler
                   {:port (:port config)
                    :join? false}))

(defn start-dev-server []
  (jetty/run-jetty (wrap-reload #'routes/handler)
                   {:port (:port config) :join? false}))

(defn -main [& args]
  (start-server))