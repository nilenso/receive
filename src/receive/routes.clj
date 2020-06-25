(ns receive.routes
  (:require [bidi.ring :refer [make-handler ->WrapMiddleware]]
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

(def logout (constantly
             {:status 302
              :headers {"Location" "/"}
              :cookies {"access_token" {:value nil
                                        :max-age 0
                                        :same-site :strict
                                        :path "/"}}
              :body ""}))

(def api-routes
  {"/ping"       api-handlers/ping
   "/download/"  {[:id ""] file-handlers/download-file}
   "/upload"     {:post {"" file-handlers/upload}}
   "/user"       {:put api-handlers/sign-in
                  :get api-handlers/fetch-user}
   "/user/files" {""  api-handlers/uploaded-files
                  "/" {[:id ""]        {:put api-handlers/update-file}
                       [:id "/shared"] {:get api-handlers/get-shared-with-users}}}
   true        api-handlers/not-found})

(def routes
  ["/" {""           file-handlers/index
        "api"        (->WrapMiddleware api-routes wrap-json-response)
        "download/"  {[:id ""] file-handlers/download-view}
        "share"      {:get file-handlers/share-handler}
        "user/files" {:get file-handlers/uploaded-files}
        "404"        {:get ui-handlers/error-page}
        "logout"     logout
        true       (constantly (response/redirect "/404"))}])

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