(ns receive.service.user-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [receive.error-handler :refer [error]]
   [receive.model.user :as model]
   [receive.service.user :as user]))

(def mock-google-user
  {:user-id 1
   :google-id "115723412421412341324"
   :dt-created #inst "2020-06-29T18:38:26.749455000-00:00"
   :dt-updated #inst "2020-06-29T18:38:26.749455000-00:00"})

(deftest check-user-exists
  (testing "returns id of a known user"
    (with-redefs [model/check-user-exists
                  (constantly mock-google-user)]
      (is (= 1
             (user/check-user-exists
              {:google-id "115723412421412341324"}))))))

(deftest signin-bad-token-test
  (is (= (user/signin-with-google "bad_token")
         (error :jwt-invalid-input))))
