(ns receive.model.file
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as result-set]
   [receive.db.connection :as connection]
   [receive.db.sql :as sql])
  (:import [java.util UUID]))

(defn is-file-owner? [{user-id :user_id} uid]
  (->> (jdbc/execute-one! connection/datasource
                          (sql/find-file (UUID/fromString uid))
                          {:return-keys true
                           :builder-fn result-set/as-unqualified-maps})
       :owner_id
       (= user-id)))