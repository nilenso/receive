(ns receive.validations
  (:require [receive.config :as config]))

(defn file-too-large?
  [handler]
  (fn [{params :params :as request}]
    (let [file (:file params)
          size (:size file)]
      (if (> size (:max-file-size config/config))
        {:status 413
         :body {:success false
                :message "File too big"}}
        (handler request)))))

(defn file-param-exists?
  [handler]
  (fn [{params :params :as request}]
    (if (:file params)
      (handler request)
      {:status 400
       :body {:success false
              :message "No file uploaded"}})))

(defn file-exists?
  [handler]
  (fn [{params :params :as request}]
    (let [file (:file params)
          size (:size file)]
      (if (> size 0)
        (handler request)
        {:status 400
         :body {:success false
                :message "File not provided"}}))))

(defn valid-filename-length?
  [handler]
  (fn [{params :params :as request}]
    (let [file (:file params)
          filename (:filename file)]
      (if (> (count filename) (:max-filename-length config/config))
        {:status 400
         :body {:success false
                :message "File name is too long"}}
        (handler request)))))

(defn upload-validation
  [handler]
  (-> handler
      file-param-exists?
      file-exists?
      valid-filename-length?
      file-too-large?))