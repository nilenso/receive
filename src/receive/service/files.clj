(ns receive.service.files
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-time.core :as time]
            [next.jdbc :as jdbc]
            [receive.db.connection :as connection]
            [receive.db.sql :as sql]
            [receive.config :as conf]
            [clj-time.coerce :as time-coerce]))

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
  [file filename]
  (jdbc/with-transaction [tx connection/datasource]
    (let [expire-in (-> conf/config :public-file :expire-in)
          dt-expire (-> expire-in
                        (time/seconds)
                        (#(time/plus (time/now) %))
                        (time-coerce/to-sql-time))
          result (jdbc/execute-one! tx
                                    (sql/save-file filename dt-expire)
                                    {:return-keys true})
          uid (:file_storage/uid result)]
      (save-to-disk file (file-save-path uid filename))
      uid)))

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