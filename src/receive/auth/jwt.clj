(ns receive.auth.jwt
  (:require [buddy.sign.jwt :as jwt]
            [clj-time.core :as time]
            [receive.config :refer [config]]
            [receive.error-handler :refer [error]]))

(defn sign [{:keys [email user-id]}]
  (let [dt-expire (time/plus (time/now)
                             (time/seconds
                              (:jwt-token-expiry config)))]
    (jwt/sign {:user_id user-id
               :email email
               :exp dt-expire}
              (-> config :secrets :jwt-secret))))

(defn verify [token]
  (try
    (jwt/unsign token (-> config :secrets :jwt-secret))
    (catch java.lang.NullPointerException _
      (error :jwt-no-token))
    (catch Exception _
      (error :jwt-expired))))