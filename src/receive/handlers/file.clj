(ns receive.handlers.file
  (:require
   [clojure.java.io :as io]
   [receive.config :refer [config]]
   [receive.error-handler :refer [error?
                                  error->http-response
                                  error->ui-response
                                  if-error]]
   [receive.handlers.helper :refer [map-response-data
                                    success]]
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
          result (files/save-file auth tempfile {:filename filename})]
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

(defn file->file-data [file]
  {:filename (:file_storage/filename file)
   :link (download-link (:file_storage/uid file))})

(defn uploaded-files-ui [{auth :auth}]
  (if auth
    (let [files (->> (:user_id auth)
                     (files/get-uploaded-files)
                     (map file->file-data))]
      (base-view/success-body-builder
       (component-view/toolbar auth)
       (file-view/file-listing files)))
    (base-view/error-ui 401 "Not authenticated")))

(defn update-file [{:keys [params route-params auth]}]
  (let [result (files/find-and-update-file auth
                                           (:id route-params)
                                           {:private? (:is_private params)
                                            :shared-with-user-emails (:shared_with_users params)})]
    (if-error result
              :http-response
              (success result))))

(defn uploaded-files [{auth :auth}]
  (if auth
    (success (->> (:user_id auth)
                  (files/get-uploaded-files)
                  (map (map-response-data :filename
                                          :uid
                                          :created_at))))
    (error->http-response {:error :unauthorized})))