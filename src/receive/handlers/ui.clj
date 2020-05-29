(ns receive.handlers.ui
  (:require
   [receive.view.base
    :refer [error-ui]]))

(defn error-page
  [_]
  {:status 200
   :body (error-ui "404" "File not found")})
