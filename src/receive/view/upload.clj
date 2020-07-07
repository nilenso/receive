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
    [:div {:class "save-settings"}
     [:button {:type "button"
               :class "small no-display"
               :id "upload-save-button"
               :onclick "saveSettings()"} "Save"]]
    [:div {:class "skip-settings"}
     [:button {:type "button"
               :class "small"
               :id "upload-get-link"
               :onclick "openShareLink()"} "Get Link"]]]
   [:span {:class "upload-error"} "File size is too big!"]])