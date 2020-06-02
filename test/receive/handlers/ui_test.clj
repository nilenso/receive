(ns receive.handlers.ui-test
  (:require [clojure.test :refer [is deftest]]
            [clojure.string :as string]
            [hiccup.core :as h]
            [receive.handlers.ui :as ui]
            [ring.mock.request :as mock]))

(deftest error-page-test
  (let [response (ui/error-page
                  (mock/request :get "/404"))
        status (:status response)
        body (:body response)]
    (is (= status 200))
    (is (string? body))
    (is (string/includes?
         body (h/html [:div {:class "error-message"}
                       [:h1 "404"]
                       [:span "File not found"]])))))

(deftest bad-link-test
  (let [response (ui/error-page
                  (mock/request :get "/really_bad_link"))
        status (:status response)
        body (:body response)]
    (is (= status 200))
    (is (string? body))
    (is (string/includes?
         body (h/html [:div {:class "error-message"}
                       [:h1 "404"]
                       [:span "File not found"]])))))