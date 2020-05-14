(ns receive.service.files
  (:require [next.jdbc :as jdbc]
            [receive.db.connection :as connection]
            [receive.db.sql :as sql]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [receive.config :as conf]))

(defn expand-home
  "Replaces the tilde in file path with the user's home directory"
  [file-name]
  (if (.startsWith file-name "~")
    (string/replace-first file-name "~" (System/getProperty "user.home"))
    file-name))

(defn file-save-path
  "Returns the path of the file to be saved given a unique ID and a file name"
  [uid filename]
  (format "%s/%s__%s" (expand-home (:storage-path conf/config)) uid filename))

(defn save-to-disk
  "Given a file and a file name, saves the files to disk"
  [tempfile filename]
  (io/copy tempfile (io/file filename)))

(defn process-uploaded-file
  "Adds a new database entry and saves file to disk"
  [file filename uid]
  (jdbc/with-transaction [tx connection/datasource]
    (let [result (jdbc/execute-one! tx (sql/save-file filename uid) {:return-keys true})]
      (save-to-disk file (file-save-path uid filename))
      result)))

(defn find-file
  "Finds the file name given a uid"
  [uid]
  (-> (jdbc/execute-one! connection/datasource (sql/find-file uid))
      :file_storage/filename))