(ns receive.handlers.file
  (:require [clojure.java.io :as io]
            [receive.config :refer [config]]
            [receive.error-handler :refer [error?
                                           error->http-response
                                           error->ui-response]]
            [receive.service.files :as files]
            [receive.spec.file :as spec]
            [receive.view.base :as base-view]
            [receive.view.components :as component-view]
            [receive.view.download :as download-view]
            [receive.view.upload :as upload-view]
            [receive.view.file :as file-view])
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
  [{params :params}]
  (if (spec/uuid-valid? params)
    (let [uid (:id params)
          abs-filename (files/get-absolute-filename uid)]
      (if (error? abs-filename)
        (error->http-response abs-filename)
        {:status 200
         :body (io/file abs-filename)}))
    (error->http-response
     {:error :invalid-uuid})))

(defn download-view [{params :params :as request}]
  (if (spec/uuid-valid? params)
    (let [uid (:id params)
          filename (files/get-filename uid)]
      (if (error? filename)
        (error->ui-response filename)
        (base-view/success-body-builder
         (component-view/toolbar (:auth request))
         (download-view/download-button uid filename))))
    (error->ui-response
     {:error :invalid-uuid})))

(defn index [request]
  (let [auth (:auth request)]
    (base-view/success-body-builder
     (component-view/toolbar auth)
     upload-view/upload-button)))

(defn download-link
  [uid]
  (format "%s/download/%s/" (:base-url config) uid))

(defn share-handler [request]
  (let [uid (-> request :params :uid)
        link (download-link uid)
        auth (:auth request)]
    (base-view/success-body-builder
     (component-view/toolbar auth)
     (upload-view/copy-button link))))

(defn file->file-data [file]
  {:filename (:file_storage/filename file)
   :link (download-link (:file_storage/uid file))})

(defn uploaded-files [{auth :auth}]
  (if auth
    (let [files (->> (:user_id auth)
                     (files/get-uploaded-files)
                     (map file->file-data))]
      (base-view/success-body-builder
       (component-view/toolbar auth)
       (file-view/file-listing files)))
    (base-view/error-ui 401 "Not authenticated")))