(ns receive.db.sql
  (:refer-clojure :exclude [update])
  (:require  [honeysql.core :as sql]
             [honeysql.helpers :refer [insert-into
                                       columns
                                       values]]))
(defn save-file
  [filename uid]
  (-> (insert-into :file-storage)
      (columns :filename :uid)
      (values [[filename uid]])
      sql/format))

(defn find-file
  [uid]
  (sql/format {:select [:filename]
               :from [:file-storage]
               :where [:= :uid uid]}))