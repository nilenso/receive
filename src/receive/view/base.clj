(ns receive.view.base
  (:require [hiccup.page :as page]))

(defn title-str [env]
  (str "Receive UI"
          (if (= env "production")
            ""
            (str " | " env))))

(defn base [env children]
  (page/html5
   [:head
    [:title (title-str env)]
    [:meta {:charset "utf-8"}]
    [:meta {:name "theme-color" :content "#5CDb95"}]
    (page/include-css "css/style.css")]
   [:body (if (= env "staging") {:class "env-staging"} {})
    [:div {:class "container"}
     children
     (page/include-js "js/main.js")]]))

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
    [:input {:id "upfile" :type "file" :name "file" :value "file" :onchange "uploadFile(this)"}]]])