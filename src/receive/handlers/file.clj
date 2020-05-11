(ns receive.handlers.file
  (:require
   [receive.service.persistence :refer [process-uploaded-file]]
   [receive.view.base
    :refer [base upload-button title]
    :rename {base base-layout}]
   [hiccup.core :as h])
  (:import
   java.util.UUID))

(defn uuid-str []
  (str (UUID/randomUUID)))

(defn upload
  "Handles file upload and saves to the location specified in the config"
  [request]
  (let [file (get (:params request) "file")
        tempfile (:tempfile file)
        filename (:filename file)
        uid (uuid-str)
        result (process-uploaded-file tempfile filename uid)]
    {:status 200
     :body {:name filename
            :uid (:file_storage/uid result)
            :success true
            :message "File saved successfully!"}}))

(defn index [_]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (h/html (base-layout [:div
                               title
                               upload-button]))})