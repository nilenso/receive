(ns receive.service.user
  (:require
   [next.jdbc :as jdbc]
   [receive.auth.jwt :as jwt]
   [receive.auth.google :as auth]
   [receive.error-handler :refer [if-error]]
   [receive.db.connection :as connection]
   [receive.model.user :as model]))

(defn check-user-exists
  [google-user]
  (:user-id
   (model/check-user-exists google-user)))

(defn get-user
  [user-id]
  (model/get-user user-id))

(defn get-user-by-email
  [tx email]
  (model/get-user-by-email tx email))

(defn create-google-user
  [tx user-id google-id]
  (model/create-google-user tx user-id google-id))

(defn create-user
  [tx user-data]
  (model/create-user tx user-data))

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
  (if-let [user-id (model/check-user-exists user-data)]
    (model/get-user user-id)
    (model/register-user user-data)))

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
