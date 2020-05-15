(ns receive.view.base
  (:require [clojure.data.json :as json]
            [clojure.walk :refer [stringify-keys]]
            [hiccup.page :as page]
            [receive.config :as config]))

(defn global-config-script []
  (-> config/config
      (select-keys [:max-file-size :max-filename-length])
      (stringify-keys)
      (json/write-str)
      (#(format "window.config = %s" %))))

(defn base [children]
  (page/html5
   [:head
    [:base {:href (:base-url config/config) :target "_blank"}]
    [:link {:rel "shortcut icon" :type "image/png" :href "favicon.svg"}]
    [:title (:ui-title config/config)]
    [:meta {:charset "utf-8"}]
    [:meta {:name "theme-color" :content "#5CDb95"}]
    (page/include-css "css/style.css")
    (page/include-js "js/main.js")
    [:script (global-config-script)]
    (page/include-js "https://cdn.jsdelivr.net/npm/axios/dist/axios.min.js")]
   [:body (if config/staging? {:class "env-staging"} {})
    [:div {:class "container"}
     children]]))

(def title
  [:a {:href (:base-url config/config)}
   [:div {:class "title-section"}
    "> Receive"]])

(def upload-button
  [:form {:action "/upload/"
          :method "post"
          :enctype "multipart/form-data"
          :name "uploadForm"}
   [:div {:class "upload-input" :onclick "getFile()"} "Upload"]
   [:div {:style "height: 0px; width: 0px; overflow: hidden"}
    [:input {:id "upfile"
             :type "file"
             :name "file"
             :value "file"
             :onchange "uploadFile(this)"}]]
   [:span {:class "upload-error"} "File size is too big!"]])

(defn download-link [uid]
  (format "/download/api/%s/" uid))

(defn download-button [uid filename]
  [:a {:download filename :href (download-link uid)}
   [:button "Download"]
   [:p filename]])

(defn copy-button
  [download-link]
  [:div {:class "download-link"}
   [:button {:id "copy-button"
             :onclick "copyLink()"} download-link]
   [:p "Click to copy"]])
