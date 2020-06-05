(ns receive.view.download)

(defn download-link [uid]
  (format "/api/download/%s/" uid))

(defn download-button [uid filename]
  [:a {:download filename :href (download-link uid)}
   [:button "Download"]
   [:p filename]])