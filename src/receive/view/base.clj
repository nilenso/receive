(ns receive.view.base
  (:require [hiccup.page :as page]
            [receive.config :as config]))

(defn base [children]
  (page/html5
   [:head
    [:base {:href (:base-url config/config) :target "_blank"}]
    [:link {:rel "shortcut icon" :type "image/png" :href "favicon.svg"}]
    [:title (:ui-title config/config)]
    [:meta {:charset "utf-8"}]
    [:meta {:name "theme-color" :content "#5CDb95"}]
    (page/include-css "css/style.css")
    (page/include-js "https://cdn.jsdelivr.net/npm/axios/dist/axios.min.js")
    (page/include-js "js/main.js")]
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
             :onchange "uploadFile(this)"}]]])

(defn copy-button
  [download-link]
  [:div {:class "download-link"}
   [:button {:id "copy-button"
             :onclick "copyLink()"} download-link]
   [:p "Click to copy"]])