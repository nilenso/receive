(ns receive.error-handler-test
  (:require [clojure.test :refer [deftest is are]]
            [receive.error-handler :as error-handler]))

(deftest error-predicate-test
  (is (true?
       (error-handler/error? (error-handler/error :jwt-expired))))
  (are [x] (false? x)
    (error-handler/error? false)
    (error-handler/error? true)))

(deftest error->http-response-test
  (is (=
       (error-handler/error->http-response (error-handler/error :jwt-invalid-input))
       {:status 400, :body {:success false, :message "Invalid JWT"}}))
  (is (=
       (error-handler/error->http-response (error-handler/error :default))
       {:status 500, :body {:success false, :message "Unknown Error"}})))

(deftest if-error-macro-test
  (is (=
       (error-handler/if-error (error-handler/error :default) :error-fn :else-fn)
       :error-fn))
  (is (=
       (error-handler/if-error (error-handler/error :default) :raise :else-fn)
       {:receive.error-handler/error :receive.error-handler/default}))
  (is (=
       (error-handler/if-error (error-handler/error :default) :http-response :else-fn)
       {:status 500, :body {:success false, :message "Unknown Error"}})))
