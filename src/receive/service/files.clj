(ns receive.service.files
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-time.coerce :as time-coerce]
            [clj-time.core :as time]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]
            [next.jdbc.sql :refer [find-by-keys]]
            [receive.db.connection :as connection]
            [receive.error-handler :refer [if-error]]
            [receive.service.user :as user]
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
  [{user-id :user_id} file {:keys [filename] :as file-data}]
  (jdbc/with-transaction [tx connection/datasource]
    (let [expire-in (-> conf/config :public-file :expire-in-sec)
          dt-expire (-> expire-in
                        (time/seconds)
                        (#(time/plus (time/now) %))
                        (time-coerce/to-sql-time))
          result (jdbc/execute-one! tx
                                    (sql/save-file user-id
                                                   file-data
                                                   dt-expire)
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

(defn update-file-data [tx uid {:keys [private? shared-with-user-ids]}]
  (-> (jdbc/execute-one! tx
                         (sql/update-file (UUID/fromString uid)
                                          {:private? private?
                                           :shared-with-users shared-with-user-ids})
                         {:return-keys true
                          :builder-fn result-set/as-unqualified-maps})
      (update :shared_with_users (comp
                                  #(map int %)
                                  #(.getArray %)))))

(defn find-and-update-file
  [{user-id :user_id :as auth} uid {:keys [private? shared-with-user-emails]}]
  (jdbc/with-transaction [tx connection/datasource]
    (if-let [file (find-file uid)]
      (if (and auth
               (= user-id (:file_storage/owner_id file)))
        (let [shared-with-user-ids (->> shared-with-user-emails
                                        (map #(user/find-or-create tx %))
                                        (map :id))]
          (update-file-data tx uid {:private? private?
                                    :shared-with-user-ids shared-with-user-ids}))
        {:error :forbidden})
      {:error :not-found})))

(defn get-shared-with-user-ids [uid]
  (-> (jdbc/execute-one! connection/datasource
                         (sql/get-shared-with-users (UUID/fromString uid)))
      :file_storage/shared_with_users
      (->> (.getArray)
           (map int))))

(defn get-shared-user-details [uid]
  (->> uid
       get-shared-with-user-ids
       (map user/get-user)))

(defn get-uploaded-files [user-id]
  (find-by-keys connection/datasource
                :file_storage
                {:owner_id user-id}))