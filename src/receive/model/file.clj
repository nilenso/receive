(ns receive.model.file
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as result-set]
   [next.jdbc.sql :refer [find-by-keys]]
   [receive.db.connection :as connection]
   [receive.db.sql :as sql])
  (:import [java.util UUID]))

(defn find-file [uid]
  (jdbc/execute-one! connection/datasource
                     (sql/find-file (UUID/fromString uid))
                     {:return-keys true
                      :builder-fn result-set/as-unqualified-maps}))

(defn save-file [tx user-id filename dt-expire]
  (jdbc/execute-one! tx
                     (sql/save-file user-id filename dt-expire)
                     {:return-keys true}))

(defn update-file-data [tx uid {:keys [private?
                                       shared-with-user-ids]}]
  (jdbc/execute-one! tx
                     (sql/update-file (UUID/fromString uid)
                                      {:private? private?
                                       :shared-with-users shared-with-user-ids})
                     {:return-keys true
                      :builder-fn result-set/as-unqualified-maps}))

(defn get-shared-with-user-ids [uid]
  (jdbc/execute-one! connection/datasource
                     (sql/get-shared-with-users
                      (UUID/fromString uid))
                     {:builder-fn result-set/as-unqualified-maps}))

(defn get-uploaded-files [user-id]
  (find-by-keys connection/datasource
                :file_storage
                {:owner_id user-id}))