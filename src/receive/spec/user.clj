(ns receive.spec.user
  (:require [clojure.spec.alpha :as s]))

(def email-regex #"^\S+@\S+\.\S+$")

(s/def ::user-id pos-int?)
(s/def ::first-name string?)
(s/def ::last-name string?)
(s/def ::email (s/and string? #(re-matches email-regex %)))
(s/def ::dt-created inst?)
(s/def ::dt-udpated inst?)
(s/def ::status string?)

(s/valid? ::email "danisam17@gmail.com")

(s/def ::db-entry (s/keys :req-un [::user-id
                                   ::first-name
                                   ::email
                                   ::dt-created
                                   ::dt-updated]
                          :opt-un [::last-name
                                   ::status]))

(defn db-entry->spec [data]
  {:user-id    (:id data)
   :first-name (:first-name data)
   :email      (:email data)
   :dt-created (:dt-created data)
   :dt-updated (:dt-updated data)
   :last-name  (:last-name data)
   :status     (:status data)})

(defn valid-db-entry? [data]
  (s/valid? ::db-entry (db-entry->spec
                        data)))

(defn valid-email? [email]
  (s/valid? ::email email))