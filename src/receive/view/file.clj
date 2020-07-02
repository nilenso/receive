(ns receive.view.file)

(defn file-item [{:keys [filename link]}]
  [:div {:class "file-item"}
   [:div {:class "filename"}
    [:span "Filename"]
    [:h4 filename]]
   [:div {:class "icons"}
    [:i.far.fa-copy
     {:onclick (format "copyToClipboard('%s')"
                       link)}]
    [:i.fas.fa-file-download
     {:onclick (format "window.open('%s')"
                       link)}]]])

(defn file-listing
  [files]
  (if (= 0 (count files))
    [:div "You have no files ¯ \\_ (ツ) _/¯"]
    [:div {:class "file-listing"}
     (map file-item files)]))