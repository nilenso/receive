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

(defmacro if-error
  "Macro. Evaluates test on the data.
   If error, evaluates `error-condition` otherwise evaluates
   the `error-condition`
   
   `error-condition` can have either an expression or `:raise` or `:http-response`
   `:raise`           returns the data
   `:http-response`   returns data wrapped with HTTP respone"
  [data error-condition else-condition]
  `(if (error? ~data)
     (case ~error-condition
       :raise ~data
       :http-response (error->http-response ~data)
       ~error-condition)
     ~else-condition))