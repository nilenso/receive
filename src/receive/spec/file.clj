(ns receive.spec.file
  (:require [clojure.spec.alpha :as s]
            [receive.config :as config]))

(s/def ::min-file-size #(> % 0))
(s/def ::max-file-size #(< % (:max-file-size config/config)))
(s/def ::max-filename-length #(< (count %)
                                 (:max-filename-length config/config)))

(s/def ::filename (s/and string?
                         ::max-filename-length))
(s/def ::content-type string?)
(s/def ::tempfile #(.exists %))
(s/def ::size (s/and ::min-file-size
                     ::max-file-size))
(s/def ::uid #(uuid? (java.util.UUID/fromString %)))

(s/def ::id integer?)
(s/def ::created-at inst?)
(s/def ::file (s/keys :req-un [::filename
                               ::content-type
                               ::size]))

(s/def ::params #(contains? % :file))

(s/def ::find-file (s/keys :req-un [::filename
                                    ::uid]
                           :opt-un [:receive.spec.user/user-id]))

(s/def ::db-entry (s/keys :req-un [::id
                                   ::filename
                                   ::uid
                                   ::created-at]
                          :opt-un [:receive.spec.user/user-id]))

(defn params-valid? [params] (s/valid? ::params params))

(defn max-file-size-valid? [params]
  (s/valid? ::max-file-size (-> params :file :size)))

(defn min-file-size-valid? [params]
  (s/valid? ::min-file-size (-> params :file :size)))

(defn max-filename-length-valid? [params]
  (s/valid? ::max-filename-length (-> params :file :filename)))

(defn db-entry->spec [data]
  {:id         (:file_storage/id data)
   :filename   (:file_storage/filename data)
   :uid        (:file_storage/uid data)
   :created-at (:file_storage/created_at data)
   :user-id    (:file_storage/user_id data)})

(defn valid-db-entry? [data]
  (s/valid? ::db-entry (db-entry->spec
                        data)))

(defn find-file-valid? [file-data]
  (s/explain ::find-file file-data))