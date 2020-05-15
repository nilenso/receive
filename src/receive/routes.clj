(ns receive.routes
  (:require [bidi.ring :refer [make-handler]]
            [receive.handlers.api :as api-handlers]
            [receive.handlers.file :as file-handlers]
            [receive.middlewares :refer [wrap-fallback-exception
                                         wrap-postgres-exception
                                         wrap-with-uri-rewrite
                                         trim-trailing-slash
                                         upload-validation]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.logger :refer [wrap-with-logger]]))

(def routes
  ["/" {:get {"" file-handlers/index}
        "api/ping" api-handlers/ping
        "upload" {:post {"" (upload-validation file-handlers/upload)}}
        "download" {"/api/" {[:id ""] file-handlers/download-file}
                    "/" {[:id ""] file-handlers/download-view}}
        "share" {:get file-handlers/share-handler}
        true api-handlers/not-found}])

(def handler
  (-> routes
      (make-handler)
      (wrap-keyword-params)
      (wrap-postgres-exception)
      (wrap-fallback-exception)
      (wrap-json-response)
      (wrap-params)
      (wrap-multipart-params)
      (wrap-with-logger)
      (wrap-with-uri-rewrite trim-trailing-slash)
      (wrap-resource "public")))