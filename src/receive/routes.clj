(ns receive.routes
  (:require [bidi.ring :refer [make-handler]]
            [receive.handlers.api :as api-handlers]
            [receive.handlers.file :as file-handlers]
            [receive.handlers.ui :as ui-handlers]
            [receive.middlewares :refer [wrap-fallback-exception
                                         wrap-postgres-exception
                                         wrap-with-uri-rewrite
                                         trim-trailing-slash
                                         verified-user
                                         wrap-cookies-keyword]]
            [ring.middleware.cookies :refer [wrap-cookies]]
            [ring.middleware.json :refer [wrap-json-response
                                          wrap-json-params]]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.logger :refer [wrap-with-logger]]
            [ring.util.response :as response]))

(def api-routes
  {"/ping"      (wrap-json-response api-handlers/ping)
   "/download/" {[:id ""] (wrap-json-response file-handlers/download-file)}
   "/upload"    {:post {"" (-> file-handlers/upload
                               (wrap-json-response))}}
   "/user"      {""        {:put (-> api-handlers/sign-in
                                     (wrap-json-response))
                            :get (-> api-handlers/fetch-user
                                     (wrap-json-response))}
                 "/files/" {[:id ""]
                            {:put (wrap-json-response
                                   api-handlers/update-file)}}}
   true       (wrap-json-response api-handlers/not-found)})

(def routes
  ["/" {""          file-handlers/index
        "api"       api-routes
        "download/" {[:id ""] file-handlers/download-view}
        "share"     {:get file-handlers/share-handler}
        "404"       {:get ui-handlers/error-page}
        true      (constantly (response/redirect "/404"))}])

(def handler
  (-> routes
      (make-handler)
      (verified-user)
      (wrap-cookies-keyword)
      (wrap-cookies)
      (wrap-keyword-params)
      (wrap-params)
      (wrap-multipart-params)
      (wrap-json-params)
      (wrap-postgres-exception)
      (wrap-fallback-exception)
      (wrap-json-response)
      (wrap-with-uri-rewrite trim-trailing-slash)
      (wrap-resource "public")
      (wrap-with-logger)))