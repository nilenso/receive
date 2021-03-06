(ns receive.view.components
  (:require [receive.service.user :as user]
            [receive.config :as config]))

(def title
  [:a {:class "title-section"
       :target "_self"
       :href (:base-url config/config)}
   [:div "> Receive"]])

(defn signin-button [auth]
  [:div {:id "btn_signin"
         :class (str "toolbar_btn "
                     (if auth "no-display" ""))}
   "Sign in"])

(defn user-button [auth]
  (let [{:keys [first-name last-name email]} (user/auth->user auth)
        full-name (format "%s %s" first-name last-name)]
    [:div {:id "btn_user"
           :class (str "toolbar_btn "
                       (if auth "" "no-display"))}
     [:div full-name]
     [:div email]
     [:ul {:class "user_menu"}
      [:li [:a {:href "/user/files" :target "_self"} "My Files"]]
      [:li [:a {:href "/logout" :target "_self"}  "Logout"]]]]))

(defn toolbar [auth]
  [:div {:class "toolbar"}
   title
   (user-button auth)
   (signin-button auth)])