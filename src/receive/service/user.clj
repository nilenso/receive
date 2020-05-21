(ns receive.service.user
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]
            [receive.auth.google :as auth]
            [receive.auth.jwt :as jwt]
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
          id (:id user)
          _ (create-google-user tx id google-id)]
      user)))

(defn create-or-fetch-user
  [user-data]
  (if-let [user-id (check-user-exists user-data)]
    (get-user user-id)
    (register-user user-data)))

(defn signin-with-google
  "If Google `id-token` verified, returns a JWT token"
  [id-token]
  (if-let [user-data (auth/verify-token id-token)]
    (-> user-data
        (create-or-fetch-user)
        (#(jwt/sign {:user-id (:id %)
                     :email (:email %)})))
    (throw (ex-info "Verification failed"
                    {:message "Can't verify user"}))))

(defn auth->user
  "Fetches the user data for authenticated user"
  [{user-id :user_id}]
  (get-user user-id))