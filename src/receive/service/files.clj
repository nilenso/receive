(ns receive.service.files
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :refer [find-by-keys]]
            [receive.db.connection :as connection]
            [receive.db.sql :as sql]
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

(defn save-file
  "Adds a new database entry and saves file to disk and returns the uid"
  [file {:keys [filename uid] :as file-data}]
  (jdbc/with-transaction [tx connection/datasource]
    (let [result (jdbc/execute-one! tx (sql/save-file file-data) {:return-keys true})]
      (save-to-disk file (file-save-path uid filename))
      (:file_storage/uid result))))

(defn get-filename
  "Finds the file name given a uid"
  [uid]
  (if-let [file (jdbc/execute-one! connection/datasource (sql/find-file uid))]
    (-> file :file_storage/filename)
    nil))

(defn get-absolute-filename
  [uid]
  (if-let [filename (get-filename uid)]
    (file-save-path uid filename)
    nil))

(defn get-uploaded-files [user-id]
  (find-by-keys connection/datasource
                :file_storage
                {:user_id user-id}))