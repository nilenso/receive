(ns receive.view.base
  (:require [hiccup.page :as page]
            [receive.config :as config]
            [receive.service.user :as user]))

(defn base [& children]
  (page/html5
   [:head
    [:base {:href (:base-url config/config) :target "_blank"}]
    [:link {:rel "shortcut icon" :type "image/png" :href "favicon.svg"}]
    [:title (:ui-title config/config)]
    [:meta {:charset "utf-8"}]
    [:meta {:name "theme-color" :content "#5CDb95"}]
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
    (page/include-js "js/signin.js")
    [:script "startApp()"]]
   [:body (if config/staging? {:class "env-staging"} {})
    [:div {:class "container"}
     children]]))

(def title
  [:a {:class "title-section"
       :href (:base-url config/config)}
   [:div "> Receive"]])

(defn signin-button [auth]
  [:div {:id "btn_signin"
         :class (str "toolbar_btn "
                     (if auth "no-display" ""))}
   "Sign in"])

(defn user-button [auth]
  (let [user (user/auth->user auth)
        first-name (:first_name user)
        last-name (:last_name user)
        full-name (format "%s %s" first-name last-name)]
    [:div {:id "btn_user"
           :class (str "toolbar_btn "
                       (if auth "" "no-display"))}
     full-name]))

(defn toolbar [auth]
  [:div {:class "toolbar"}
   title
   (user-button auth)
   (signin-button auth)])

(def upload-button
  [:form {:action "/api/upload/"
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
  (format "/api/download/%s/" uid))

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
