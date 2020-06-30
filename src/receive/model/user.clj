(ns receive.model.user
  (:require
   [next.jdbc :as jdbc]
   [receive.db.connection :as connection]
   [receive.db.helper :refer [as-unqualified-kebab-maps]]
   [receive.db.sql :as sql]))

(def db-options
  {:builder-fn as-unqualified-kebab-maps
   :return-keys true})

(defn check-user-exists
  [{google-id :google-id}]
  (jdbc/execute-one! connection/datasource
                     (sql/get-google-user google-id)
                     db-options))

(defn get-user
  [user-id]
  (jdbc/execute-one! connection/datasource
                     (sql/get-user user-id)
                     db-options))

(defn get-user-by-email [tx email]
  (jdbc/execute-one! tx
                     (sql/get-user-by-email email)
                     db-options))

(defn create-google-user
  [tx user-id google-id]
  (jdbc/execute-one! tx
                     (sql/create-google-user user-id google-id)
                     db-options))

(defn create-user
  [tx user-data]
  (jdbc/execute-one! tx
                     (sql/create-user user-data)
                     db-options))