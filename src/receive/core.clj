(ns receive.core
  (:require
   [bidi.ring :refer [make-handler]]
   [clojure.string :as string]
   [hiccup.core :as h]
   [receive.config :refer [config]]
   [receive.service.persistence :refer [process-uploaded-file]]
   [receive.view.base
    :refer [base upload-button title copy-button]
    :rename {base base-layout}]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.json :refer [wrap-json-response]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
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
   :body (h/html (base-layout [:div
                               title
                               upload-button]))})

(defn download-link
  [uid]
  (format "%s/download/%s/" (:base-url config) uid))

(defn share-handler [request]
  (let [uid (-> request :params :uid)
        link (download-link uid)]
    {:status 200
     :body (h/html (base-layout [:div
                                 title
                                 (copy-button link)]))}))

(def handler
  (make-handler ["/" {:get {"" index
                            "ping" ping}
                      "upload" {:post upload}
                      "share" {:get share-handler}
                      true not-found}]))

(defn wrap-trailing-slash
  [handler]
  (fn [request]
    (let [uri (:uri request)
          trailing-slash-matcher #".+/"
          trailing-slash? (re-matches trailing-slash-matcher uri)
          remove-trailing-slash #(string/join "" (drop-last %))]
      (if trailing-slash?
        (handler (assoc request :uri (remove-trailing-slash uri)))
        (handler request)))))

(def app (-> handler
             (wrap-postgres-exception)
             (wrap-fallback-exception)
             (wrap-keyword-params)
             (wrap-json-response)
             (wrap-params)
             (wrap-multipart-params)
             (wrap-with-logger)
             (wrap-keyword-params)
             (wrap-trailing-slash)
             (wrap-resource "public")))

(defn start-server
  []
  (jetty/run-jetty app {:port  (:port config)
                        :join? false}))

(defn start-dev-server []
  (jetty/run-jetty (wrap-reload #'app) {:port (:port config) :join? false}))

(defn -main [& args]
  (start-server))