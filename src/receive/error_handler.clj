(ns receive.error-handler
  (:require [receive.view.base :refer [error-body-builder]]))

(defonce error-map
  {::jwt-invalid-input    {:message "Invalid JWT"
                           :status  400}
   ::jwt-no-token         {:message "No token provided"
                           :status  400}
   ::jwt-bad-token        {:message "JWT format is incorrect"
                           :status  400}
   ::jwt-expired          {:message "Token has expired"
                           :status  401}
   ::file-expired         {:message "Link has expired"
                           :status  410}
   ::not-found            {:message "File not found"
                           :status  404}
   ::invalid-uuid         {:message "Not valid UUID"
                           :status  400}
   ::forbidden            {:message "You do not have access to this content"
                           :status  403}
   ::unauthorized         {:message "Not authenticated"
                           :status  401}
   ::invalid-email-domain {:message "Only same domain emails allowed"
                           :status  400}
   ::bad-email            {:message "Bad email address"
                           :status  400}
   ::default              {:message "Unknown Error"
                           :status  500}})

(defonce current-ns "receive.error-handler")

(defn error? [data]
  (keyword? (::error data)))

(defn error
  "Returns an error hash-map. 
   Throws error if `code` does not exists in error-map"
  [code]
  (let [error-code (keyword current-ns (name code))]
    (assert (error-code error-map)
            "Error `code` provided does not exist in `error-map`")
    {::error error-code}))

(def not-error? (complement error?))

(defn error->http-response
  "Returns a ring error response based on error key.
   Defaults to server error"
  [{error-code ::error}]
  (if error-code
    (if-let [response-data (error-code error-map)]
      {:status (:status response-data)
       :body {:success false
              :message (:message response-data)}}
      (error->http-response (error :default)))
    (error->http-response (error :default))))

(defn error->ui-response
  "Returns a ring HTML error response
   
   Accepts the error hash-map and and function `error-body-builder`
   
   `error-body-builder` function should accept `status` and `message`
   `status`:     HTTP status code
   `message`:    Message to be displayed"
  [{error-code ::error}]
  (if error-code
    (if-let [response-data (error-code error-map)]
      (let [status (:status response-data)
            message (:message response-data)]
        {:status (:status response-data)
         :body (error-body-builder status message)})
      (error->ui-response (error :default)))
    (error->ui-response (error :default))))

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