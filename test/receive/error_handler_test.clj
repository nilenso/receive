(ns receive.error-handler-test
  (:require [clojure.test :refer [deftest is]]
            [receive.error-handler :as error-handler]))

(deftest error-predicate-test
  (is (true?
       (error-handler/error? {:error :jwt-expired})))
  (is (false?
       (error-handler/error? {:error 'jwt-expired})))
  (is (false?
       (error-handler/error? false)))
  (is (false?
       (error-handler/error? true)))
  (is (false?
       (error-handler/error? {:error nil}))))

(deftest error->http-response-test
  (is (=
       (error-handler/error->http-response {:error :jwt-invalid-input})
       {:status 400 :body {:success false :message "Invalid JWT"}}))
  (is (=
       (error-handler/error->http-response {:error nil})
       {:status 500 :body {:success false :message "Unknown Error"}})))

(deftest if-error-macro-test
  (is (=
       (error-handler/if-error {:error :error-code} :error-fn :else-fn)
       :error-fn))
  (is (=
       (error-handler/if-error {:error :error-code} :raise :else-fn)
       {:error :error-code}))
  (is (=
       (error-handler/if-error {:error :error-code} :http-response :else-fn)
       {:status 500 :body {:success false :message "Unknown Error"}})))