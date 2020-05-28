(ns receive.routes
  (:require [bidi.ring :refer [make-handler]]
            [receive.handlers.api :as api-handlers]
            [receive.handlers.file :as file-handlers]
            [receive.handlers.ui :as ui-handlers]
            [receive.middlewares :refer [wrap-fallback-exception
                                         wrap-postgres-exception
                                         wrap-with-uri-rewrite
                                         trim-trailing-slash]]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.logger :refer [wrap-with-logger]]
            [ring.util.response :as response]))

(def api-routes
  {"/ping" (wrap-json-response api-handlers/ping)
   "/download/" {[:id ""] file-handlers/download-file}
   "/upload" {:post {"" (-> file-handlers/upload
                            (wrap-json-response))}}
   true (wrap-json-response api-handlers/not-found)})

(def routes
  ["/" {"" file-handlers/index
        "api" api-routes
        "download/"  {[:id ""] file-handlers/download-view}
        "share" {:get file-handlers/share-handler}
        "404" {:get ui-handlers/error-page}
        true (constantly (response/redirect "/404"))}])

(def handler
  (-> routes
      (make-handler)
      (wrap-keyword-params)
      (wrap-postgres-exception)
      (wrap-fallback-exception)
      (wrap-params)
      (wrap-multipart-params)
      (wrap-with-uri-rewrite trim-trailing-slash)
      (wrap-resource "public")
      (wrap-with-logger)))