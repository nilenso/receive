(ns receive.view.base
  (:require [hiccup.page :as page]
            [receive.config :as config]
            [clojure.data.json :as json]
            [clojure.walk :refer [stringify-keys]]))

(defn global-config-script []
  (-> config/config
      (select-keys [:max-file-size :max-filename-length])
      (stringify-keys)
      (json/write-str)
      (#(format "window.config = %s" %))))

(defn base [children]
  (page/html5
   [:head
    [:title (:ui-title config/config)]
    [:meta {:charset "utf-8"}]
    [:meta {:name "theme-color" :content "#5CDb95"}]
    (page/include-css "css/style.css")
    (page/include-js "js/main.js")
    [:script (global-config-script)]]
   [:body (if config/staging? {:class "env-staging"} {})
    [:div {:class "container"}
     children]]))

(def title
  [:a {:href "http://receive.nilenso.com"}
   [:div {:class "title-section"}
    "> Receive"]])

(def upload-button
  [:form {:action "/upload/"
          :method "post"
          :enctype "multipart/form-data"
          :name "myForm"}
   [:div {:class "upload-input" :onclick "getFile()"} "Upload"]
   [:div {:style "height: 0px; width: 0px; overflow: hidden"}
    [:input {:id "upfile" :type "file" :name "file" :value "file" :onchange "uploadFile(this)"}]]
   [:span {:class "upload-error"} "File size is too big!"]])