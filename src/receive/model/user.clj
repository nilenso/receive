(ns receive.model.user
  (:require
   [next.jdbc :as jdbc]
   [next.jdbc.result-set :as result-set]
   [receive.db.connection :as connection]
   [receive.db.sql :as sql]))

(defn check-user-exists
  [{google-id :google-id}]
  (:account_google/user_id
   (jdbc/execute-one! connection/datasource
                      (sql/get-google-user google-id))))

(defn get-user
  [user-id]
  (jdbc/execute-one! connection/datasource
                     (sql/get-user user-id)
                     {:return-keys true
                      :builder-fn result-set/as-unqualified-maps}))

(defn create-google-user
  [tx user-id google-id]
  (jdbc/execute-one! tx
                     (sql/create-google-user user-id google-id)
                     {:return-keys true}))

(defn create-user
  [tx {email :email
       first-name :first-name
       last-name :last-name}]
  (jdbc/execute-one! tx
                     (sql/create-user first-name
                                      last-name
                                      email)
                     {:return-keys true
                      :builder-fn result-set/as-unqualified-maps}))

(defn register-user
  [{google-id :google-id :as user}]
  (jdbc/with-transaction [tx connection/datasource]
    (let [user (create-user tx user)
          id (:id user)]
      (create-google-user tx id google-id)
      user)))