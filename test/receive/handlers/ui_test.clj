(ns receive.handlers.ui-test
  (:require [clojure.test :refer [is deftest]]
            [receive.handlers.ui :as ui]
            [ring.mock.request :as mock]))

(deftest error-page-test
  (let [response (ui/error-page
                  (mock/request :get "/404"))
        status (:status response)
        body (:body response)]
    (is (= status 200))
    (is (string? body))))

(deftest bad-link-test
  (let [response (ui/error-page
                  (mock/request :get "/404"))
        status (:status response)
        body (:body response)]
    (is (= status 200))
    (is (string? body))))