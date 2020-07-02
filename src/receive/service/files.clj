(ns receive.service.files
  (:require
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.tools.logging :as log]
   [clj-time.coerce :as time-coerce]
   [clj-time.core :as time]
   [next.jdbc :as jdbc]
   [receive.config :as conf]
   [receive.db.connection :as connection]
   [receive.error-handler :refer [if-error]]
   [receive.model.file :as model]
   [receive.service.user :as user]))

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
  [{user-id :user_id} file {:keys [filename] :as _file-data}]
  (jdbc/with-transaction [tx connection/datasource]
    (let [expire-in (-> conf/config :public-file :expire-in-sec)
          dt-expire (-> expire-in
                        (time/seconds)
                        (#(time/plus (time/now) %))
                        (time-coerce/to-sql-time))
          result (model/save-file tx user-id filename dt-expire)
          uid (:uid result)]
      (save-to-disk file (file-save-path (str uid) filename))
      uid)))

(defn get-filename
  "Finds the file name given a uid"
  [uid]
  (if-let [response (model/find-file uid)]
    (let [file (:filename response)
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

(defn delete-file-and-db-entry! [{:keys [filename uid]}]
  (log/info "Deleting file" filename uid)
  (let [file-path (file-save-path uid filename)]
    (jdbc/with-transaction [tx connection/datasource]
      (model/delete-file tx uid)
      (when (.exists (io/file file-path))
        (io/delete-file file-path)))))

(defn purge-expired-files! []
  (log/info "Purging")
  (let [files (model/find-expired-files)]
    (doseq [file files]
      (delete-file-and-db-entry! file))))

(defn update-file-data [tx uid file-data]
  (clojure.tools.logging/warn "file_data" file-data)
  (model/update-file-data tx uid file-data))

(defn find-and-update-file
  [{user-id :user_id :as auth} uid {:keys [private? shared-with-user-emails]}]
  (jdbc/with-transaction [tx connection/datasource]
    (if-let [file (model/find-file uid)]
      (if (and auth
               (= user-id (:owner-id file)))
        (let [shared-with-user-ids (->> shared-with-user-emails
                                        (map #(user/find-or-create tx %))
                                        (map :id))]
          (update-file-data tx uid {:private? private?
                                    :shared-with-user-ids shared-with-user-ids}))
        {:error :forbidden})
      {:error :not-found})))

(defn is-owner? [user-id owner-id]
  (= user-id owner-id))

(defn is-file-owner? [{user-id :user_id} uid]
  (->> (model/find-file uid)
       :owner-id
       (is-owner? user-id)))

(defn is-shared-with? [user-id shared-with-users]
  (when shared-with-users
    (some #(= user-id %) shared-with-users)))

(defn has-read-access? [{user-id :user_id} uid]
  (let [{:keys [is-private owner-id shared-with-users]}
        (model/find-file uid)]
    (or (not is-private)
        (is-owner? user-id owner-id)
        (is-shared-with? user-id shared-with-users))))

(defn get-uploaded-files [user-id]
  (model/get-uploaded-files user-id))
