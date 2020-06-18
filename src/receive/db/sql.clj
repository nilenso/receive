(ns receive.db.sql
  (:refer-clojure :exclude [update])
  (:require  [honeysql.core :as sql]
             [honeysql.types :refer [array]]
             [honeysql.helpers :refer [insert-into
                                       columns
                                       values
                                       where
                                       update
                                       sset]]
             [honeysql-postgres.helpers :as psqlh]))

(defn save-file
  [filename dt-expire user-id]
  (-> (insert-into :file-storage)
      (columns :filename :dt_expire :owner_id)
      (values [[filename (sql/call :cast dt-expire
                                   :timestamp) user-id]])
      sql/format))

(defn find-file
  [uid]
  (sql/format {:select [:filename
                        :owner_id
                        :shared_with_users
                        [(sql/call :< :dt_expire (sql/call :now)) :expired]]
               :from   [:file-storage]
               :where  [:= :uid uid]}))

(defn get-google-user
  [google-id]
  (sql/format {:select [:*]
               :from [:account_google]
               :where [:= :google_id google-id]}))

(defn get-user
  [user-id]
  (sql/format {:select [:*]
               :from [:users]
               :where [:= :id user-id]}))

(defn get-user-by-email
  [email]
  (sql/format {:select [:*]
               :from [:users]
               :where [:= :email email]}))

(defn create-user
  [{:keys [first-name last-name email status]}]
  (-> (insert-into :users)
      (columns :first_name :last_name :email :status)
      (values [[first-name
                last-name
                email
                (sql/call :cast (or status "active")
                          :user_status)]])
      (psqlh/upsert (-> (psqlh/on-conflict :email)
                        (psqlh/do-update-set :first_name :last_name :status)))
      (sql/format)))

(defn create-google-user
  [user-id google-id]
  (-> (insert-into :account_google)
      (columns :user_id :google_id)
      (values [[user-id google-id]])
      (sql/format)))

(defn update-file [uid {:keys [private? shared-with-users]}]
  (-> (update :file_storage)
      (sset {:is_private private?
             :shared_with_users (array shared-with-users)})
      (where [:= :uid uid])
      (sql/format)))