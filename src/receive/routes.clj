(ns receive.routes
  (:require [bidi.ring :refer [make-handler]]
            [receive.handlers.api :as api-handlers]
            [receive.handlers.file :as file-handlers]
            [receive.middlewares :refer [wrap-fallback-exception
                                         wrap-postgres-exception]]
            [receive.validations :as validations]
            [ring.middleware.json :refer [wrap-json-response]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.logger :refer [wrap-with-logger]]))

(def routes
  ["/" {:get {"" file-handlers/index
              "ping" api-handlers/ping}
        "upload" {:post {"/"
                         (validations/upload-validation file-handlers/upload)}}
        true api-handlers/not-found}])

(def handler
  (-> routes
      (make-handler)
      (wrap-postgres-exception)
      (wrap-fallback-exception)
      (wrap-json-response)
      (wrap-params)
      (wrap-multipart-params)
      (wrap-with-logger)
      (wrap-resource "public")))