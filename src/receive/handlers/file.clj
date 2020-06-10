(ns receive.handlers.file
  (:require [clojure.java.io :as io]
            [hiccup.core :as h]
            [receive.config :refer [config]]
            [receive.service.files :as files]
            [receive.spec.file :as spec]
            [receive.view.base
             :refer [base upload-button
                     toolbar download-button
                     copy-button
                     error-ui
                     file-listing]
             :rename {base base-layout}]
            [ring.util.response :as response])
  (:import java.util.UUID))

(defn uuid-str []
  (str (UUID/randomUUID)))

(def response-no-file-uploaded
  {:status 400
   :body {:success false
          :message "No file uploaded"}})

(def response-file-too-large
  {:status 413
   :body {:success false
          :message "File too big"}})

(def response-file-not-provided
  {:status 400
   :body {:success false
          :message "File not provided"}})

(def response-filename-too-long
  {:status 400
   :body {:success false
          :message "File name is too long"}})

(defn upload
  "Handles file upload and saves to the location specified in the config"
  [{:keys [params auth] :as request}]
  (cond
    (not (spec/params-valid? params)) response-no-file-uploaded
    (not (spec/max-file-size-valid? params)) response-file-too-large
    (not (spec/min-file-size-valid? params)) response-file-not-provided
    (not (spec/max-filename-length-valid? params)) response-filename-too-long
    :else
    (let [{:keys [tempfile filename]
           :as _file} (-> request :params :file)
          result (files/save-file tempfile {:filename filename
                                            :uid (uuid-str)
                                            :user-id (:user_id auth)})]
      {:status 200
       :body {:name filename
              :uid result
              :success true
              :message "File saved successfully!"}})))

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
        filename (files/get-filename uid)
        auth (:auth request)]
    (if filename
      {:status 200
       :headers {"Content-Type" "text/html"}
       :body (h/html (base-layout [:div
                                   (toolbar auth)
                                   (download-button uid filename)]))}
      (response/redirect "/404"))))

(defn index [request]
  (let [auth (:auth request)]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (h/html (base-layout [:div
                                 (toolbar auth)
                                 upload-button]))}))

(defn download-link
  [uid]
  (format "%s/download/%s/" (:base-url config) uid))

(defn share-handler [request]
  (let [uid (-> request :params :uid)
        link (download-link uid)
        auth (:auth request)]
    {:status 200
     :body (h/html (base-layout [:div
                                 (toolbar auth)
                                 (copy-button link)]))}))

(defn map-file-data [files]
  (map
   (fn [file]
     {:filename (:file_storage/filename file)
      :link (download-link (:file_storage/uid file))})
   files))

(defn uploaded-files [{auth :auth}]
  (if auth
    (let [files (map-file-data
                 (files/get-uploaded-files (:user_id auth)))]
      {:status 200
       :body (h/html (base-layout
                      (toolbar auth)
                      (file-listing files)))})
    (error-ui 401 "Not authenticated")))