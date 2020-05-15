(ns receive.core
  (:require
   [bidi.ring :refer [make-handler]]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [hiccup.core :as h]
   [receive.config :refer [config]]
   [receive.service.files :as files]
   [receive.view.base
    :refer [base upload-button title
            download-button copy-button]
    :rename {base base-layout}]
   [ring.adapter.jetty :as jetty]
   [ring.middleware.json :refer [wrap-json-response]]
   [ring.middleware.params :refer [wrap-params]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.reload :refer [wrap-reload]]
   [ring.logger :refer [wrap-with-logger]]
   [clojure.tools.logging :as log]
   [ring.util.response :as response])
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
  (let [file (-> request :params :file)
        tempfile (:tempfile file)
        filename (:filename file)
        uid (uuid-str)
        result (files/save-file tempfile filename uid)]
    {:status 200
     :body {:name filename
            :uid result
            :success true
            :message "File saved successfully!"}}))

(defn wrap-fallback-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e)
        {:status 500 :body {:success false
                            :message "Server error"}}))))

(defn wrap-postgres-exception
  [handler]
  (fn [request]
    (try
      (handler request)
      (catch org.postgresql.util.PSQLException e
        (log/error e)
        {:status 400
         :body {:success false
                :message "Invalid data"}}))))

(defn download-file
  [request]
  (let [uid (-> request :params :id)
        abs-filename (files/get-absolute-filename uid)]
    (if abs-filename
      {:status 200
       :body (io/file abs-filename)}
      {:status 404
       :body {:message "File not found"
              :success false}})))

(defn download-view [request]
  (let [uid (-> request :params :id)
        filename (files/get-filename uid)]
    (if filename
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (h/html (base-layout [:div
                                   title
                                   (download-button uid filename)]))}
      (response/redirect "/404"))))

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
  (make-handler ["/" {:get {"" index}
                      "api/ping" ping
                      "upload" {:post upload}
                      "download" {"/api/" {[:id ""] download-file}
                                  "/" {[:id ""] download-view}}
                      "share" {:get share-handler}
                      true not-found}]))

(defn trim-trailing-slash [uri]
  (when (and (not= uri "/")
             (.endsWith uri "/"))
    (-> uri drop-last string/join)))

(defn wrap-with-uri-rewrite [handler f]
  (fn [{uri :uri :as request}]
    (if-let [rewrite (f uri)]
      (handler (assoc request :uri rewrite))
      (handler request))))

(def app (-> handler
             (wrap-postgres-exception)
             (wrap-fallback-exception)
             (wrap-keyword-params)
             (wrap-json-response)
             (wrap-params)
             (wrap-multipart-params)
             (wrap-with-logger)
             (wrap-keyword-params)
             (wrap-with-uri-rewrite trim-trailing-slash)
             (wrap-resource "public")))

(defn start-server
  []
  (jetty/run-jetty app {:port (:port config)
                        :join? false}))

(defn start-dev-server []
  (jetty/run-jetty (wrap-reload #'app) {:port (:port config) :join? false}))

(defn -main [& args]
  (start-server))