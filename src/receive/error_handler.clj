(ns receive.error-handler)

(def errors
  {:jwt-invalid-input {:message "Invalid JWT"
                       :status  400}
   :jwt-no-token      {:message "No token provided"
                       :status  400}
   :jwt-bad-token     {:message "JWT format is incorrect"
                       :status  400}
   :jwt-expired       {:message "Token has expired"
                       :status  401}})

(defn error? [data]
  (keyword? (:error data)))

(def not-error? (complement error?))

(defn error->http-response
  [{error-code :error}]
  (let [response-data (error-code errors)]
    {:status (:status response-data)
     :body {:success false
            :message (:message response-data)}}))