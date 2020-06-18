(ns receive.service.files
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-time.coerce :as time-coerce]
            [clj-time.core :as time]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]
            [receive.db.connection :as connection]
            [receive.error-handler :refer [if-error]]
            [receive.db.sql :as sql]
            [receive.config :as conf])
  (:import [java.util UUID]))

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
    (let [expire-in (-> conf/config :public-file :expire-in-sec)
          dt-expire (-> expire-in
                        (time/seconds)
                        (#(time/plus (time/now) %))
                        (time-coerce/to-sql-time))
          result (jdbc/execute-one! tx
                                    (sql/save-file filename dt-expire)
                                    {:return-keys true})
          uid (:file_storage/uid result)]
      (save-to-disk file (file-save-path uid filename))
      (str uid))))

(defn find-file
  [uid]
  (jdbc/execute-one! connection/datasource
                     (sql/find-file (UUID/fromString uid))))

(defn get-filename
  "Finds the file name given a uid"
  [uid]
  (if-let [response (find-file uid)]
    (let [file (:file_storage/filename response)
          expired? (:expired response)]
      (if expired?
        {:error :file-expired}
        file))
    {:error :not-found}))

(defn get-absolute-filename
  [uid]
  (let [filename (get-filename uid)]
    (if-error filename
              :raise
              (file-save-path uid filename))))

(defn find-expired-files []
  (jdbc/execute! connection/datasource
                 (sql/find-expired-files)
                 {:builder-fn result-set/as-unqualified-maps}))

(defn delete-db-entry [tx uid]
  (jdbc/execute-one! tx (sql/delete-file uid)))

(defn delete-file-and-db-entry [{:keys [filename uid]}]
  (let [file-path (file-save-path uid filename)]
    (jdbc/with-transaction [tx connection/datasource]
      (delete-db-entry tx uid)
      (when (.exists (io/file file-path))
        (io/delete-file file-path)))))

(defn purge-expired-files []
  (let [files (find-expired-files)]
    (map delete-file-and-db-entry files)))