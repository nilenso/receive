(ns receive.validations
  (:require [receive.config :as config]))

(defn validate-file-too-large
  [handler]
  (fn [{params :params :as request}]
    (let [file (:file params)
          size (:size file)]
      (if (> size (:max-file-size config/config))
        {:status 413
         :body {:success false
                :message "File too big"}}
        (handler request)))))

(defn validate-upload-request-params
  [handler]
  (fn [{params :params :as request}]
    (if (:file params)
      (handler request)
      {:status 400
       :body {:success false
              :message "No file uploaded"}})))

(defn validate-file-exists
  [handler]
  (fn [{params :params :as request}]
    (let [file (:file params)
          size (:size file)]
      (if (> size 0)
        (handler request)
        {:status 400
         :body {:success false
                :message "File not provided"}}))))

(defn validate-filename-length
  [handler]
  (fn [{params :params :as request}]
    (let [file (:file params)
          filename (:filename file)]
      (if (> (count filename) (:max-filename-length config/config))
        {:status 400
         :body {:success false
                :message "File name is too long"}}
        (handler request)))))