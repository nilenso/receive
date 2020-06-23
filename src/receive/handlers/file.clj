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
            [receive.view.upload :as upload-view])
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
  [{params :params auth :auth :as request}]
  (cond
    (not (spec/params-valid? params)) response-no-file-uploaded
    (not (spec/max-file-size-valid? params)) response-file-too-large
    (not (spec/min-file-size-valid? params)) response-file-not-provided
    (not (spec/max-filename-length-valid? params)) response-filename-too-long
    :else
    (let [file (-> request :params :file)
          tempfile (:tempfile file)
          filename (:filename file)
          result (files/save-file auth tempfile filename)]
      {:status 200
       :body {:name filename
              :uid result
              :success true
              :message "File saved successfully!"}})))

(defn download-file
  [{:keys [params auth]}]
  (if (spec/uuid-valid? params)
    (let [uid (:id params)]
      (if (files/has-read-access? auth uid)
        (let [abs-filename (files/get-absolute-filename uid)]
          (if (error? abs-filename)
            (error->http-response abs-filename)
            {:status 200
             :body (io/file abs-filename)}))
        (error->http-response {:error :forbidden})))
    (error->http-response {:error :invalid-uuid})))

(defn download-view [{:keys [params auth] :as request}]
  (if (spec/uuid-valid? params)
    (let [uid (:id params)]
      (if (files/has-read-access? auth uid)
        (let [filename (files/get-filename uid)]
          (if (error? filename)
            (error->ui-response filename)
            (base-view/success-body-builder
             (component-view/toolbar (:auth request))
             (download-view/download-button uid filename))))
        (error->ui-response {:error :forbidden})))
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
