(ns receive.auth.jwt
  (:require [buddy.sign.jwt :as jwt]
            [clj-time.core :as time]
            [receive.config :refer [config]]))

(defn sign [{user-id :user-id
             email :email}]
  (jwt/sign
   {:user_id user-id
    :email email
    :exp (time/plus (time/now)
                    (time/seconds (:jwt-token-expiry config)))}
   (-> config :secrets :jwt-secret)))

(defn verify [token]
  (jwt/unsign token (-> config :secrets :jwt-secret)))