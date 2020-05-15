(ns receive.handlers.file
  (:require [clojure.java.io :as io]
            [hiccup.core :as h]
            [receive.config :refer [config]]
            [receive.service.files :as files]
            [receive.view.base
             :refer [base upload-button
                     title download-button
                     copy-button]
             :rename {base base-layout}]
            [ring.util.response :as response])
  (:import java.util.UUID))

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