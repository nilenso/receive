(ns receive.service.persistence
  (:require [clojure.java.io :as io]
            [clojure.string :refer [replace-first]]
            [receive.util.config :refer [config]]
            [next.jdbc :as jdbc]
            [receive.db.connection :refer [datasource]]
            [receive.db.sql :refer [save-file]]))

(defn expand-home
  "Replaces the tilde in file path with the user's home directory"
  [file-name]
  (if (.startsWith file-name "~")
    (replace-first file-name "~" (System/getProperty "user.home"))
    file-name))

(defn file-save-path
  "Returns the path of the file to be saved given a unique ID and a file name"
  [uid filename]
  (format "%s/%s__%s" (expand-home (:storage-path config)) uid filename))

(defn save-to-disk
  "Given a file and a file name, saves the files to disk"
  [tempfile filename]
  (io/copy tempfile (io/file filename)))

(defn process-uploaded-file
  [file filename uid]
  (jdbc/with-transaction [tx datasource]
    (let [result (jdbc/execute-one! tx (save-file filename uid) {:return-keys true})]
      (save-to-disk file (file-save-path uid filename))
      result)))