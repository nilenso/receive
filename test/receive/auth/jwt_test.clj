(ns receive.auth.jwt-test
  (:require [clojure.test :refer [deftest is]]
            [receive.auth.jwt :as jwt]))

(deftest jwt-test
  (let [user-data {:user-id 100
                   :email "john@titor.com"}
        verified (-> user-data
                     jwt/sign
                     jwt/verify)]
    (is (= (:user-id user-data) (:user_id verified)))
    (is (= (:email user-data) (:email verified)))
    (is (contains? verified :exp))))