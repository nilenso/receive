(ns receive.model.user-test
  (:require
   [clojure.test :refer [deftest is testing use-fixtures]]
   [next.jdbc.sql :refer [insert! delete!]]
   [receive.db.connection :refer [datasource]]
   [receive.factory :as factory]
   [receive.model.user :as model]
   [receive.util :as util]))

(def ^:dynamic *user-data* nil)

(defn create-user [user]
  (insert! datasource :users
           (util/keywords->sql-keywords user)))

(defn delete-user [{user-id :users/id}]
  (delete! datasource :users {:id user-id}))

(deftest get-user-test
  (testing "should return the user data given user ID"
    (let [{:keys [first_name last_name email]}
          (model/get-user (:users/id *user-data*))]
      (is (= first_name (:users/first_name *user-data*)))
      (is (= last_name (:users/last_name *user-data*)))
      (is (= email (:users/email *user-data*))))))

(deftest create-user-test
  (testing "should create a user and return correct data"
    (let [user-data (factory/generate-user)
          user (model/create-user datasource user-data)]
      (is (:id user))
      (is (:first-name user-data) (:first-name user))
      (is (:last-name user-data) (:last-name user))
      (is (:email user-data) (:email user))
      (is (inst? (:dt_created user)))
      (is (inst? (:dt_updated user)))
      (delete-user {:users/id (:id user)}))))

(defn user-fixture [f]
  (let [user (create-user (factory/generate-user))]
    (binding [*user-data* user]
      (f))
    (delete-user user)))

(use-fixtures :each user-fixture)