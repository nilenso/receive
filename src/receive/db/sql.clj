(ns receive.db.sql
  (:refer-clojure :exclude [update])
  (:require  [honeysql.core :as sql]
             [honeysql.helpers :refer [insert-into
                                       columns
                                       values]]))
(defn save-file
  [filename dt-expire]
  (-> (insert-into :file-storage)
      (columns :filename :dt_expire)
      (values [[filename (sql/call :cast dt-expire
                                   :timestamp)]])
      sql/format))

(defn find-file
  [uid]
  (sql/format {:select [:filename]
               :from   [:file-storage]
               :where  [:= :uid
                        (sql/call :cast uid :uuid)]}))