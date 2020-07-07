(ns receive.view.file
  (:require
   [clojure.string :as str]))

(defn file-item [{:keys [filename link uid]}]
  [:div {:class "file-item"}
   [:div {:class "filename"}
    [:span "Filename"]
    [:h4 filename]]
   [:div {:class "icons"}
    [:i.far.fa-copy
     {:onclick (format "copyToClipboard('%s')"
                       link)}]
    [:i.fas.fa-file-download
     {:onclick (format "window.open('api/download/%s')"
                       uid)}]
    [:i.fas.fa-chevron-right
     {:onclick (format "window.open('user/files/%s')"
                       uid)}]]])

(defn file-listing
  [files]
  (if (= 0 (count files))
    [:div "You have no files ¯ \\_ (ツ) _/¯"]
    [:div {:class "file-listing"}
     (map file-item files)]))

(defn file-details
  [{:keys [filename shared-with-users]}]
  (let [emails (map :email shared-with-users)]
    [:div {:class "file-details"}
     [:button {:class "file-name"}
      [:div filename]]
     [:div {:class "shared-with"}
      [:label "Shared with"]
      [:textarea {:type "text"
                  :name "text"
                  :class "vertical-only"
                  :disabled "disabled"
                  :autocomplete "off"
                  :id "shared-with-emails"}
       (if (empty? emails)
         "You have not shared the file with anyone"
         (str/join ", " emails))]]]))