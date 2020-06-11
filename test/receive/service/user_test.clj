(ns receive.service.user-test
  (:require [clojure.test :refer [deftest is]]
            [receive.service.user :as user]))

(deftest signin-bad-token-test
  (is (= (user/signin-with-google "bad_token")
         {:error :jwt-invalid-input})))
