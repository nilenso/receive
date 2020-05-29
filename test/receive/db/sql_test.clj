(ns receive.db.sql-test
  (:require [clojure.test :refer [deftest is]]
            [receive.db.sql :as sql]))

(deftest save-file
  (is (= (sql/save-file
          "filename"
          "bb5c2dd1-26d7-4f64-9957-09932e4ede41")
         ["INSERT INTO file_storage (filename, dt_expire) VALUES (?, CAST(? AS timestamp))" "filename" "bb5c2dd1-26d7-4f64-9957-09932e4ede41"])))

(deftest find-file
  (is (= (sql/find-file
          "bb5c2dd1-26d7-4f64-9957-09932e4ede41")
         ["SELECT filename, dt_expire < now() AS expired FROM file_storage WHERE uid = CAST(? AS uuid)"
          "bb5c2dd1-26d7-4f64-9957-09932e4ede41"])))