(ns receive.db.sql
  (:refer-clojure :exclude [update])
  (:require
   [clj-time.coerce :as time-coerce]
   [honeysql.core :as sql]
   [honeysql-postgres.format]
   [honeysql-postgres.helpers :as psqlh]
   [honeysql.helpers :refer [insert-into
                             columns
                             values
                             where
                             update
                             sset]]))

(defn save-file
  [filename dt-expire user-id]
  (-> (insert-into :file-storage)
      (columns :filename :dt-expire :owner-id)
      (values [[filename (sql/call :cast (time-coerce/to-sql-time dt-expire)
                                   :timestamp) user-id]])
      sql/format))

(defn get-file [uid]
  (sql/format {:select [:*]
               :from [:file-storage]
               :where [:= :uid uid]}))

(defn find-file
  [uid]
  (sql/format {:select [:filename
                        :owner-id
                        :shared-with-users
                        :is-private
                        [(sql/call :< :dt-expire (sql/call :now)) :expired]]
               :from   [:file-storage]
               :where  [:= :uid uid]}))

(defn find-expired-files
  []
  (sql/format {:select [:filename :uid]
               :from [:file-storage]
               :where (sql/call :< :dt_expire (sql/call :now))}))

(defn delete-file [uid]
  (sql/format {:delete []
               :from [:file-storage]
               :where [:= :uid uid]}))

(defn get-google-user
  [google-id]
  (sql/format {:select [:*]
               :from [:account-google]
               :where [:= :google-id google-id]}))

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
  (-> (insert-into :account-google)
      (columns :user-id :google-id)
      (values [[user-id google-id]])
      (sql/format)))

(defn update-file [uid {:keys [private? shared-with-users dt-expire]}]
  (-> (update :file-storage)
      (sset {:is-private (sql/call :coalesce private? :is-private)
             :shared-with-users (if shared-with-users
                                  (into-array Integer/TYPE shared-with-users)
                                  (sql/call :coalesce nil
                                            :shared-with-users))
             :dt-expire (if (= dt-expire :no-update)
                          (sql/call :coalesce nil :dt-expire)
                          (time-coerce/to-sql-time dt-expire))})
      (where [:= :uid uid])
      (sql/format)))

(defn get-shared-with-users [uid]
  (sql/format {:select [:shared-with-users :owner-id]
               :from [:file-storage]
               :where [:= :uid uid]}))