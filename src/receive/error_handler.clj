(ns receive.error-handler
  (:require [hiccup.core :as h]
            [receive.view.base
             :refer [base error-ui]
             :rename {base base-layout}]))

(defonce error-map
  {:jwt-invalid-input {:message "Invalid JWT"
                       :status  400}
   :jwt-no-token      {:message "No token provided"
                       :status  400}
   :jwt-bad-token     {:message "JWT format is incorrect"
                       :status  400}
   :jwt-expired       {:message "Token has expired"
                       :status  401}
   :file-expired      {:message "Link has expired"
                       :status  410}
   :not-found         {:message "File not found"
                       :status  404}
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

(defn error->ui-response
  [{error-code :error}]
  (if error-code
    (if-let [response-data (error-code error-map)]
      (let [status (:status response-data)
            message (:message response-data)]
        {:status (:status response-data)
         :body (h/html (base-layout [:div
                                     (error-ui status message)]))})
      (error->ui-response {:error :default}))
    (error->ui-response {:error :default})))