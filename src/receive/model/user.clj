(ns receive.model.user
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as result-set]
   [receive.db.connection :as connection]
   [receive.db.sql :as sql]))

(defn check-user-exists
  [{google-id :google-id}]
  (jdbc/execute-one! connection/datasource
                     (sql/get-google-user google-id)
                     {:builder-fn result-set/as-unqualified-maps}))

(defn get-user
  [user-id]
  (jdbc/execute-one! connection/datasource
                     (sql/get-user user-id)
                     {:return-keys true
                      :builder-fn result-set/as-unqualified-maps}))

(defn get-user-by-email [tx email]
  (jdbc/execute-one! tx
                     (sql/get-user-by-email email)
                     {:return-keys true
                      :builder-fn result-set/as-unqualified-maps}))

(defn create-google-user
  [tx user-id google-id]
  (jdbc/execute-one! tx
                     (sql/create-google-user user-id google-id)
                     {:return-keys true}))

(defn create-user
  [tx user-data]
  (jdbc/execute-one! tx
                     (sql/create-user user-data)
                     {:return-keys true
                      :builder-fn result-set/as-unqualified-maps}))