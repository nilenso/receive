(ns receive.db.sql
  (:refer-clojure :exclude [update])
  (:require  [honeysql.core :as sql]
             [honeysql.helpers :refer [insert-into
                                       columns
                                       values]]))
(defn save-file
  [{:keys [filename user-id] :as _file-data} dt-expire]
  (-> (insert-into :file-storage)
      (columns :filename :user_id :dt_expire)
      (values [[filename user-id (sql/call :cast dt-expire
                                           :timestamp)]])
      sql/format))

(defn find-file
  [uid]
  (sql/format {:select [:filename
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

(defn create-user
  [first-name last-name email]
  (-> (insert-into :users)
      (columns :first_name :last_name :email)
      (values [[first-name last-name email]])
      (sql/format)))

(defn create-google-user
  [user-id google-id]
  (-> (insert-into :account_google)
      (columns :user_id :google_id)
      (values [[user-id google-id]])
      (sql/format)))
