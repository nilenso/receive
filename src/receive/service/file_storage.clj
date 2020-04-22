(ns receive.service.file-storage
  (:require [next.jdbc :as jdbc]
            [receive.db.connection :as connection]
            [receive.db.sql :as sql]))

(defn find-file
  [uid]
  (:file_storage/filename
   (jdbc/execute-one! connection/datasource (sql/find-file uid))))