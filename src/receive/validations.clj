(ns receive.validations
  (:require [receive.config :as config]))

(defn file-too-large?
  [handler]
  (fn [{params :params :as request}]
    (let [file (get params "file")
          size (:size file)]
      (if (> size (:max-file-size config/config))
        {:status 413
         :body {:success false
                :message "File too big"}}
        (handler request)))))

(defn file-param-exists?
  [handler]
  (fn [{params :params :as request}]
    (if (get params "file")
      (handler request)
      {:status 400
       :body {:success false
              :message "No file uploaded"}})))

(defn file-exists?
  [handler]
  (fn [{params :params :as request}]
    (let [file (get params "file")
          size (:size file)]
      (if (> size 0)
        (handler request)
        {:status 400
         :body {:success false
                :message "File not provided"}}))))

(defn upload-validation
  [handler]
  (-> handler
      file-param-exists?
      file-exists?
      file-too-large?))