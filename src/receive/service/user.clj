(ns receive.service.user
  (:require [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]
            [receive.auth.google :as auth]
            [receive.error-handler :refer [if-error]]
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

(defn get-user-by-email
  [tx email]
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

(defn create-unregistered-user
  "Creates a user with unregistered status"
  [tx email]
  (create-user tx  {:first-name email
                    :email email
                    :status "unregistered"}))

(defn register-user
  [{google-id :google-id :as user}]
  (jdbc/with-transaction [tx connection/datasource]
    (let [user (create-user tx user)
          id (:id user)]
      (create-google-user tx id google-id)
      user)))

(defn create-or-fetch-user
  [user-data]
  (if-let [user-id (check-user-exists user-data)]
    (get-user user-id)
    (register-user user-data)))

(defn signin-with-google
  "If Google `id-token` verified, returns a JWT token"
  [id-token]
  (let [user-data (auth/verify-token id-token)]
    (if-error user-data
              :raise
              (-> user-data
                  (create-or-fetch-user)
                  (#(jwt/sign {:user-id (:id %)
                               :email (:email %)}))))))

(defn auth->user
  "Fetches the user data for authenticated user"
  [{user-id :user_id}]
  (get-user user-id))

(defn find-or-create
  "Search for a user by email or creates an unregistered user"
  [tx email]
  (or (get-user-by-email tx email)
      (create-unregistered-user tx email)))