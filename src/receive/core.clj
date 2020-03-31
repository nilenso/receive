(ns receive.core
  (:require [ring.adapter.jetty :as jetty]
            [bidi.ring :refer (make-handler)]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [clojure.java.io :as io]
            [clojure.string :refer [replace-first]]))

(defn expand-home
  "Replaces the tilde in file path with the user's home directory"
  [file-name]
  (if (.startsWith file-name "~")
    (replace-first file-name "~" (System/getProperty "user.home"))
    file-name))

(defn uuid []
  (.toString (java.util.UUID/randomUUID)))

(defn file-save-path
  "Returns the path of the file to be saved given a unique ID and a file name"
  [uid filename]
  (format "%s%s__%s" (expand-home "~/Desktop/") uid filename))

(defn save-to-disk
  "Given a file and a file name, saves the files to disk"
  [tempfile filename]
  (io/copy tempfile (io/file filename)))

(def ping (constantly
           {:status 200
            :body {:success true
                   :message "Server is running fine!"}}))

(def not-found
  (constantly {:status 404
               :body {:success false
                      :message "Not found"}}))

(defn upload 
  "Handles file upload and saves to the location specified in the config"
  [request]
  (let [file (get (:params request) "file")
        tempfile (:tempfile file)
        filename (:filename file)
        uid (uuid)]
    (save-to-disk tempfile (file-save-path uid filename))
    {:status 200
     :body {:name filename}}))

(def handler
  (make-handler ["/" {:get {"ping" ping}
                      "upload" {:post {"/" upload}}
                      true not-found}]))

(def app (-> handler
             wrap-json-response
             wrap-params
             wrap-multipart-params))

(defn start-server
  []
  (jetty/run-jetty app {:port  3000
                        :join? false}))

(defn start-dev-server []
  (jetty/run-jetty (wrap-reload #'app) {:port 3000 :join? false}))

(defn -main [& args]
  (start-server))