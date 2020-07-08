(ns receive.auth.google
  (:require [receive.config :refer [config]]
            [receive.error-handler :refer [error]])
  (:import [com.google.api.client.googleapis.auth.oauth2
            GoogleIdTokenVerifier$Builder]
           [com.google.api.client.json.jackson2 JacksonFactory]
           [com.google.api.client.http.javanet NetHttpTransport]))

(defonce json-factory (JacksonFactory.))
(defonce transport (NetHttpTransport.))
(defonce client-id (-> config :secrets :google-credentials :client-id))
(defonce verifier (.. (GoogleIdTokenVerifier$Builder. transport json-factory)
                      (setAudience (list client-id))
                      (build)))

(defn payload->user-info
  [payload]
  {:email (.getEmail payload)
   :google-id (.getUserId payload)
   :first-name (.get payload "given_name")
   :last-name (.get payload "family_name")})

(defn verify-token [token]
  (try
    (if-let [verified-token (.verify verifier token)]
      (-> verified-token
          (.getPayload)
          (payload->user-info))
      (error :jwt-expired))
    (catch java.lang.IllegalArgumentException _
      (error :jwt-invalid-input))
    (catch java.lang.NullPointerException _
      (error :jwt-no-token))
    (catch Exception _
      (error :jwt-bad-token))))