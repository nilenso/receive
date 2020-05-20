(ns receive.db.sql-test
  (:require [clojure.test :refer [deftest is]]
            [receive.db.sql :as sql]))

(deftest save-file
  (is (= (sql/save-file
          "filename"
          "bb5c2dd1-26d7-4f64-9957-09932e4ede41")
         ["INSERT INTO file_storage (filename, uid) VALUES (?, ?)" "filename" "bb5c2dd1-26d7-4f64-9957-09932e4ede41"])))

(deftest find-file
  (is (= (sql/find-file
          "bb5c2dd1-26d7-4f64-9957-09932e4ede41")
         ["SELECT filename FROM file_storage WHERE uid = ?" "bb5c2dd1-26d7-4f64-9957-09932e4ede41"])))

(deftest get-google-user-test
  (is (=  (sql/get-google-user 1)
          ["SELECT * FROM account_google WHERE google_id = ?" 1])))

(deftest get-user-test
  (is (=  (sql/get-user 1)
          ["SELECT * FROM users WHERE id = ?" 1])))

(deftest create-user-test
  (is (= (sql/create-user "first_name" "last_name" "first_last@mail.com")
         ["INSERT INTO users (first_name, last_name, email) VALUES (?, ?, ?)" "first_name" "last_name" "first_last@mail.com"])))

(deftest create-google-user-test
  (is (= (sql/create-google-user 1 2)
         ["INSERT INTO account_google (user_id, google_id) VALUES (?, ?)" 1 2])))