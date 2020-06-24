(ns receive.spec.user
  (:require [clojure.spec.alpha :as s]))

(def email-regex #"^\S+@\S+\.\S+$")

(s/def ::user-id pos-int?)
(s/def ::first-name string?)
(s/def ::last-name string?)
(s/def ::email (s/and string? #(re-matches email-regex %)))
(s/def ::dt-created inst?)

(s/valid? ::email "danisam17@gmail.com")