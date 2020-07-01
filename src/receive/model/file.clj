(ns receive.model.file
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.sql :refer [find-by-keys]]
   [receive.db.connection :as connection]
   [receive.db.helper :refer [as-unqualified-kebab-maps]]
   [receive.db.sql :as sql])
  (:import
   [java.util UUID]))

(def db-options
  {:builder-fn as-unqualified-kebab-maps
   :return-keys true})

(defn find-expired-files []
  (jdbc/execute! connection/datasource
                 (sql/find-expired-files)
                 db-options))

(defn delete-db-entry [tx uid]
  (jdbc/execute-one! tx (sql/delete-file uid)))

(defn find-file [uid]
  (jdbc/execute-one! connection/datasource
                     (sql/find-file (UUID/fromString uid))
                     db-options))

(defn save-file [tx user-id filename dt-expire]
  (jdbc/execute-one! tx
                     (sql/save-file filename dt-expire user-id)
                     db-options))

(defn update-file-data [tx uid {:keys [private?
                                       shared-with-user-ids]}]
  (jdbc/execute-one! tx
                     (sql/update-file (UUID/fromString uid)
                                      {:private? private?
                                       :shared-with-users shared-with-user-ids})
                     db-options))

(defn get-uploaded-files [user-id]
  (find-by-keys connection/datasource
                :file_storage
                {:owner_id user-id}
                db-options))
