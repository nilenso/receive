(ns receive.spec.user
  (:require [clojure.spec.alpha :as s]))

(s/def ::user-id integer?)
(s/def ::first-name string?)
(s/def ::last-name string?)
(s/def ::email string?)
(s/def ::dt-created inst?)