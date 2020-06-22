(ns receive.spec.file
  (:require [clojure.spec.alpha :as s]
            [receive.config :as config])
  (:import [java.util UUID]))

(s/def ::min-file-size #(> % 0))
(s/def ::max-file-size #(< % (:max-file-size config/config)))
(s/def ::min-filename-length #(> (count %) 0))
(s/def ::max-filename-length #(< (count %)
                                 (:max-filename-length config/config)))

(s/def ::filename (s/and string?
                         ::min-filename-length
                         ::max-filename-length))
(s/def ::content-type string?)
(s/def ::tempfile #(.exists %))
(s/def ::size (s/and ::min-file-size
                     ::max-file-size))
(s/def ::uid (fn [uuid]
               (uuid? (try (UUID/fromString uuid)
                           (catch Exception _ false)))))
(s/def ::db-uid uuid?)

(s/def ::id integer?)
(s/def ::dt-created inst?)
(s/def ::date inst?)
(s/def ::nil nil?)
(s/def ::dt-expire (s/or ::date ::nil))
(s/def ::user-id (s/or ::id ::nil))
(s/def ::file (s/keys :req-un [::filename
                               ::content-type
                               ::size]))

(s/def ::params #(contains? % :file))

(s/def ::find-file (s/keys :req-un [::filename
                                    ::uid]
                           :opt-un [:receive.spec.user/user-id]))

(s/def ::db-entry (s/keys :req-un [::filename
                                   ::db-uid
                                   ::dt-created
                                   ::user-id]
                          :opt-un [:receive.spec.user/user-id
                                   ::dt-expire]))

(defn params-valid? [params] (s/valid? ::params params))

(defn max-file-size-valid? [params]
  (s/valid? ::max-file-size (-> params :file :size)))

(defn min-file-size-valid? [params]
  (s/valid? ::min-file-size (-> params :file :size)))

(defn max-filename-length-valid? [params]
  (s/valid? ::max-filename-length (-> params :file :filename)))

(defn db-entry->spec [data]
  {:filename   (:file_storage/filename data)
   :db-uid     (:file_storage/uid data)
   :dt-created (:file_storage/dt_created data)
   :dt-expire  (:file_storage/dt_expire data)
   :user-id    (:file_storage/user_id data)})

(defn valid-db-entry? [data]
  (s/valid? ::db-entry (db-entry->spec
                        data)))

(defn find-file-valid? [file-data]
  (s/explain ::find-file file-data))

(defn uuid-valid? [params]
  (s/valid? ::uid (-> params :id)))
