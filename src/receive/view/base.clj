(ns receive.view.base
  (:require
   [hiccup.core :as h]
   [hiccup.page :as page]
   [receive.config :as config]))

(def head
  [:head
   [:base {:href (:base-url config/config) :target "_blank"}]
   [:link {:rel "shortcut icon" :type "image/png" :href "favicon.svg"}]
   [:title (:ui-title config/config)]
   [:meta {:charset "utf-8"}]
   [:meta {:name "theme-color" :content "#5CDb95"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   [:meta {:name "google-signin-client_id"
           :content (-> config/config
                        :secrets
                        :google-credentials
                        :client-id)}]
   (page/include-css "css/style.css")
   (page/include-js "https://cdn.jsdelivr.net/npm/axios/dist/axios.min.js")
   (page/include-js "js/config.js")
   (page/include-js "js/main.js")
   [:script {:src "https://apis.google.com/js/api:client.js"}]
   [:script {:src "https://kit.fontawesome.com/3547196ebb.js"}]
   (page/include-js "js/signin.js")
   [:script "startApp()"]])

(defn base [& children]
  (page/html5
   head
   [:body
    (if config/staging? {:class "env-staging"} {})
    [:div {:class "container"}
     children]]))

(defn error-message
  [error-code error-message]
  [:div {:class "error-message"}
   [:h1 error-code]
   [:span error-message]])

(defn error-ui
  [code message]
  (h/html
   (base [:div
          (error-message code message)])))

(defn success-body-builder [& items]
  {:status 200
   :headers {"Content-Type" "text/html"}
   :body (h/html (base items))})

(defn error-body-builder [status message]
  (h/html (base [:div
                 (error-ui status message)])))