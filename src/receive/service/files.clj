(ns receive.service.files
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-time.coerce :as time-coerce]
            [clj-time.core :as time]
            [next.jdbc :as jdbc]
            [receive.db.connection :as connection]
            [receive.error-handler :refer [if-error]]
            [receive.service.user :as user]
            [receive.model.file :as model]
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
  [{user-id :user_id} file {:keys [filename] :as _file-data}]
  (jdbc/with-transaction [tx connection/datasource]
    (let [expire-in (-> conf/config :public-file :expire-in-sec)
          dt-expire (-> expire-in
                        (time/seconds)
                        (#(time/plus (time/now) %))
                        (time-coerce/to-sql-time))
          result (model/save-file tx user-id filename dt-expire)
          uid (:file_storage/uid result)]
      (save-to-disk file (file-save-path uid filename))
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

(defn update-file-data [tx uid file-data]
  (-> (model/update-file-data tx uid file-data)
      (update :shared_with_users (comp
                                  #(map int %)
                                  #(.getArray %)))))

(defn find-and-update-file
  [{user-id :user_id :as auth} uid {:keys [private? shared-with-user-emails]}]
  (jdbc/with-transaction [tx connection/datasource]
    (if-let [file (model/find-file uid)]
      (if (and auth
               (= user-id (:owner_id file)))
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
       :owner_id
       (is-owner? user-id)))

(defn is-shared-with? [user-id shared-with-users]
  (if shared-with-users
    (let [ids (->> shared-with-users
                   (.getArray)
                   (map int))]
      (some #(= user-id %) ids))
    false))

(defn has-read-access? [{user-id :user_id} uid]
  (let [file (model/find-file uid)]
    (if (:is_private file)
      (if (is-owner? user-id (:owner_id file))
        true
        (if (is-shared-with? user-id (:shared_with_users file))
          true
          false))
      true)))

(defn get-uploaded-files [user-id]
  (model/get-uploaded-files user-id))
