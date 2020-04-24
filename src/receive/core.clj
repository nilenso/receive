(ns receive.core
  (:require
   [bidi.ring :refer [make-handler]]
   [hiccup.core :as h]
   [receive.config :refer [config]]
   [receive.service.persistence :refer [process-uploaded-file]]
   [receive.view.base
    :refer [base upload-button title]
    :rename {base base-layout}]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.json :refer [wrap-json-response]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.logger :refer [wrap-with-logger]])
  (:import
   java.util.UUID))

(def ping (constantly
           {:status 200
            :body {:success true
                   :message "Server is running fine!"}}))

(def not-found
  (constantly {:status 404
               :body {:success false
                      :message "Not found"}}))

(defn uuid-str []
  (str (UUID/randomUUID)))

(defn upload
  "Handles file upload and saves to the location specified in the config"
  [request]
  (let [file (get (:params request) "file")
        tempfile (:tempfile file)
        filename (:filename file)
        uid (uuid-str)
        result (process-uploaded-file tempfile filename uid)]
    {:status 200
     :body {:name filename
            :uid (:file_storage/uid result)
            :success true
            :message "File saved successfully!"}}))

(defn wrap-fallback-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception _
        {:status 500 :body {:success false
                            :message "Server error"}}))))

(defn wrap-postgres-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch org.postgresql.util.PSQLException _
        {:status 400
         :body {:success false
                :message "Invalid data"}}))))

(defn index [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (h/html (base-layout (:env config) [:div
                               title
                               upload-button]))})

(def handler
  (make-handler ["/" {:get {"" index
                            "ping" ping}
                      "upload" {:post {"/" upload}}
                      true not-found}]))

(def app (-> handler
             wrap-postgres-exception
             wrap-fallback-exception
             wrap-json-response
             wrap-params
             wrap-multipart-params
             wrap-with-logger
             (wrap-resource "public")))

(defn start-server
  []
  (jetty/run-jetty app {:port  (:port config)
                        :join? false}))

(defn start-dev-server []
  (jetty/run-jetty (wrap-reload #'app) {:port (:port config) :join? false}))

(defn -main [& args]
  (start-server))