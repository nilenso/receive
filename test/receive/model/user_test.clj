(ns receive.model.user-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [next.jdbc.sql :refer [insert! delete!]]
   [receive.db.connection :refer [datasource]]
   [receive.factory :as factory]
   [receive.model.user :as model]
   [receive.spec.user :as spec]
   [receive.util :as util]))

(def ^:dynamic *user-data* nil)

(defn create-user [user]
  (insert! datasource :users
           (util/keywords->sql-keywords user)))

(defn delete-user [{user-id :id}]
  (delete! datasource :users {:id user-id}))

(defn user-fixture [f]
  (let [user (create-user (factory/generate-user))]
    (binding [*user-data* user]
      (f))
    (delete-user user)))

(use-fixtures :each user-fixture)

(deftest get-user-test
  (testing "should return the user data given user ID"
    (let [{:keys [first-name last-name email]}
          (model/get-user (:id *user-data*))]
      (is (= first-name (:first-name *user-data*)))
      (is (= last-name (:last-name *user-data*)))
      (is (= email (:email *user-data*)))))
  (testing "should return valid entries"
    (is (true? (spec/valid-db-entry? (model/get-user
                                      (:id *user-data*)))))))

(deftest create-user-test
  (testing "should create a user and return correct data"
    (let [user-data (factory/generate-user)
          user (model/create-user datasource user-data)]
      (is (:id user))
      (is (:first-name user-data) (:first-name user))
      (is (:last-name user-data) (:last-name user))
      (is (:email user-data) (:email user))
      (is (inst? (:dt-created user)))
      (is (inst? (:dt-updated user)))
      (delete-user {:id (:id user)}))))