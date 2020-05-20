(ns receive.auth.google
 (:require [receive.config :refer [config]])
  (:import [com.google.api.client.googleapis.auth.oauth2
            #_GoogleIdToken
            #_GoogleIdToken$Payload
            GoogleIdTokenVerifier$Builder
            #_GoogleIdTokenVerifier]
           [com.google.api.client.json.jackson2 JacksonFactory]
           [com.google.api.client.http.javanet NetHttpTransport]))

(defonce json-factory (JacksonFactory.))
(defonce transport (NetHttpTransport.))
(defonce client-id (-> config :secrets :google-credentials :client-id))
(defonce verifier (.. (GoogleIdTokenVerifier$Builder. transport json-factory)
                      (setAudience (list client-id))
                      (build)))

(defn payload->user-data
  [payload]
  {:email (.getEmail payload)
   :google-id (.getUserId payload)
   :first-name (.get payload "given_name")
   :last-name (.get payload "family_name")})

(defn verify-token [token]
  (when-let [verified-token (.verify verifier token)]
    (-> verified-token
        (.getPayload)
        (payload->user-data))))