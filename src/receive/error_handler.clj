(ns receive.error-handler)

(defonce error-map
  {:jwt-invalid-input {:message "Invalid JWT"
                       :status  400}
   :jwt-no-token      {:message "No token provided"
                       :status  400}
   :jwt-bad-token     {:message "JWT format is incorrect"
                       :status  400}
   :jwt-expired       {:message "Token has expired"
                       :status  401}
   :default           {:message "Unknown Error"
                       :status  500}})

(defn error? [data]
  (keyword? (:error data)))

(def not-error? (complement error?))

(defn error->http-response
  "Returns a ring error response based on error key.
   Defaults to server error"
  [{error-code :error}]
  (if error-code
    (if-let [response-data (error-code error-map)]
      {:status (:status response-data)
       :body {:success false
              :message (:message response-data)}}
      (error->http-response {:error :default}))
    (error->http-response {:error :default})))

(defmacro if-error
  "Macro. Evaluates test on the data.
   If error, evaluates `error-condition` otherwise evaluates
   the `else-condition`
   
   `error-condition` can have either an expression or `:raise` or `:http-response`
   `:raise`           returns the data
   `:http-response`   returns data wrapped with HTTP respone"
  [data error-condition else-condition]
  `(if (error? ~data)
     (let [error-condition# ~error-condition]
       (case error-condition#
         :raise ~data
         :http-response (error->http-response ~data)
         error-condition#))
     ~else-condition))