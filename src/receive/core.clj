(ns receive.core
  (:require [ring.adapter.jetty :as jetty]
            [bidi.ring :refer (make-handler)]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.middleware.resource :refer [wrap-resource]]
            [clojure.java.io :as io]
            [clojure.string :refer [replace-first]]
            [hiccup.core :as h]
            [receive.view.base :refer [base upload-button title] :rename {base base-layout}]))

(defn expand-home [file-name]
  (if (.startsWith file-name "~")
    (replace-first file-name "~" (System/getProperty "user.home"))
    file-name))

(defn uuid []
  (.toString (java.util.UUID/randomUUID)))

(defn save-to-disk [tempfile uid filename]
  (io/copy tempfile (io/file (str (expand-home "~/Desktop/") uid "__" filename))))

(def ping (constantly
           {:status 200
            :body {:success true
                   :message "Server is running fine!"}}))

(def not-found
  (constantly {:status 404
               :body {:success false
                      :message "Not found"}}))

(defn upload [request]
  (let [file (get (:params request) "file")
        tempfile (:tempfile file)
        filename (:filename file)
        uid (uuid)]
    (save-to-disk tempfile uid filename)
    {:status 200
     :body {:name filename}}))

(defn index [request]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (h/html (base-layout [:div
                               title
                               upload-button]))})

(def handler
  (make-handler ["/"
                 {:get {"" index
                        "ping" ping}
                  "upload" {:post {"/" upload}}
                  true not-found}]))

(def app (-> handler
             wrap-json-response
             wrap-params
             wrap-multipart-params
             (wrap-resource "public")))

(defn start-server
  []
  (jetty/run-jetty app {:port  3000
                        :join? false}))

(defn start-dev-server []
  (jetty/run-jetty (wrap-reload #'app) {:port 3000 :join? false}))

(defn -main [& args]
  (start-server))