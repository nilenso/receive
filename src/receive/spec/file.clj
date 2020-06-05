(ns receive.spec.file
  (:require [clojure.spec.alpha :as s]
            [receive.config :as config])
  (:import [java.util UUID]))

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
(s/def ::uid (fn [uuid] 
               (uuid? (try (UUID/fromString uuid)
                           (catch Exception _ false)))))

(s/def ::file (s/keys :req-un [::filename
                               ::content-type
                               ::size]))

(s/def ::params #(contains? % :file))

(defn params-valid? [params] (s/valid? ::params params))

(defn max-file-size-valid? [params]
  (s/valid? ::max-file-size (-> params :file :size)))

(defn min-file-size-valid? [params]
  (s/valid? ::min-file-size (-> params :file :size)))

(defn max-filename-length-valid? [params]
  (s/valid? ::max-filename-length (-> params :file :filename)))

(defn uuid-valid? [params]
  (s/valid? ::uid (-> params :id)))