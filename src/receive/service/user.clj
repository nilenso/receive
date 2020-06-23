(ns receive.service.user
  (:require
   [receive.auth.google :as auth]
   [receive.auth.jwt :as jwt]
   [receive.error-handler :refer [if-error]]
   [receive.model.user :as model]))

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
  (model/get-user user-id))

(defn get-user [user-id]
  (model/get-user user-id))