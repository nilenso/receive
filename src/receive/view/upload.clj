(ns receive.view.upload)

(defn copy-button
  [download-link]
  [:div {:class "download-link"}
   [:button {:id "copy-button"
             :onclick "copyLink()"} download-link]
   [:p "Click to copy"]])

(defn upload-button [auth]
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
             :onchange (if auth "uploadFile(this, true)"
                           "uploadFile(this, false)")}]]
   [:div {:class "private-upload"}
    [:div {:class "toggle"}
     [:label "Share privately?"]
     [:input {:type "checkbox"
              :autocomplete "off"
              :onchange "onIsPrivateToggle(this)"
              :id "is-private"}]]
    [:div
     [:input {:type "text"
              :name "text"
              :class "no-display"
              :autocomplete "off"
              :id "shared-with-emails"
              :placeholder "Enter Emails separated by commas"}]]
    [:div {:class "expiry-settings"}
     [:label {:class "select"}
      [:select {:name "file-expiry"
                :autocomplete "off"
                :id "expire-in"}
       [:option {:value (* 60 60)} "Expire in 1 Hour"]
       [:option {:value (* 60 60 3)} "Expire in 3 Hours"]
       [:option {:value (* 60 60 8)} "Expire in 8 Hours"]
       [:option {:value (* 60 60 24)} "Expire in 1 day"]
       [:option {:value (* 60 60 24 5)} "Expire in 5 days"]
       [:option {:value (* 60 60 24 12)} "Expire in 12 days"]
       [:option {:value 0 :selected "selected"} "Don't expire"]]]]
    [:div {:class "save-settings"}
     [:button {:type "button"
               :class "small no-display"
               :id "upload-save-button"
               :onclick "saveSettings()"} "Save"]]
    [:div {:class "skip-settings"}
     [:button {:type "button"
               :class "small"
               :id "upload-get-link"
               :onclick "saveSettings()"} "Get Link"]]]
   [:span {:class "upload-error"} "File size is too big!"]])