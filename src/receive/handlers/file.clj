(ns receive.handlers.file
  (:require
   [clojure.java.io :as io]
   [receive.config :refer [config]]
   [receive.error-handler :refer [error?
                                  error->http-response
                                  error->ui-response
                                  if-error
                                  error]]
   [receive.handlers.helper :refer [map-response-data
                                    success]]
   [receive.service.files :as files]
   [receive.spec.file :as spec]
   [receive.spec.user :refer [valid-email?]]
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
      (success result))))

(defn download-file
  [{:keys [params auth]}]
  (if (spec/uuid-valid? params)
    (let [uid (:id params)]
      (if (files/has-read-access? auth uid)
        (let [filename (files/get-filename uid)]
          (if-error filename
                    :http-response
                    {:status 200
                     :headers {"Content-Disposition"
                               (format "attachment; filename=\"%s\"" filename)}
                     :body (io/file (files/file-save-path uid filename))}))
        (error->http-response (error :forbidden))))
    (error->http-response (error :invalid-uuid))))

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
        (error->ui-response (error :forbidden))))
    (error->ui-response
     (error :invalid-uuid))))

(defn index [request]
  (let [auth (:auth request)]
    (base-view/success-body-builder
     (component-view/toolbar auth)
     (upload-view/upload-button auth))))

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
  (assoc file :link (download-link (:uid file))))

(defn uploaded-files-ui [{auth :auth}]
  (if auth
    (let [files (->> (:user-id auth)
                     (files/get-uploaded-files)
                     (map file->file-data))]
      (base-view/success-body-builder
       (component-view/toolbar auth)
       (file-view/file-listing files)))
    (base-view/error-ui 401 "Not authenticated")))

(defn- emails-valid? [emails]
  (every? valid-email? emails))

(defn- domain-regex [domain]
  (re-pattern (str "^.*@" domain "$")))

(defn- allowed-domain? [email]
  (re-matches (domain-regex (:domain config))
              email))

(defn- all-domains-allowed? [emails]
  (if (:domain-locked config)
    (every? allowed-domain? emails)
    true))

(defn update-file [{:keys [route-params auth]
                    {shared-with-emails :shared_with_users
                     private? :is_private
                     dt-expire :dt_expire
                     :or {dt-expire :no-update}} :params}]
  (cond
    (not (all-domains-allowed?
          shared-with-emails)) (error->http-response
                                (error :invalid-email-domain))
    (not (emails-valid?
          shared-with-emails)) (error->http-response
                                (error :bad-email))
    :else
    (let [result (files/find-and-update-file auth
                                             (:id route-params)
                                             {:private? private?
                                              :shared-with-user-emails shared-with-emails
                                              :dt-expire dt-expire})]
      (if-error result
                :http-response
                (success result)))))

(defn uploaded-files [{auth :auth}]
  (if auth
    (success (->> (:user-id auth)
                  (files/get-uploaded-files)
                  (map (map-response-data :filename
                                          :uid
                                          :created_at))))
    (error->http-response (error :unauthorized))))

(defn get-shared-with-users [{:keys [route-params auth]}]
  (let [uid (:id route-params)
        is-owner? (files/is-file-owner? auth uid)]
    (if is-owner?
      (let [result (files/get-shared-user-details (:id route-params))]
        (if-error result
                  :http-response
                  (success result)))
      (error->http-response (error :forbidden)))))

(defn file-details-ui [{:keys [route-params auth]}]
  (let [uid (:id route-params)
        is-owner? (files/is-file-owner? auth uid)]
    (if is-owner?
      (base-view/success-body-builder
       (component-view/toolbar auth)
       (file-view/file-details
        (-> (files/find-file uid)
            (assoc :shared-with-users
                   (files/get-shared-user-details uid)))))
      (error->ui-response (error :forbidden)))))
