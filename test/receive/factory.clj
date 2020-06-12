(ns receive.factory
  (:require
   [clojure.string :as string]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen]
   [receive.spec.user]))

(def non-blank-str?
  (fn [s]
    (not (string/blank? s))))

(def lower-case?
  (fn [s]
    (every? #(Character/isLowerCase %) s)))

(def email-gen
  (s/gen
   (s/with-gen :receive.spec.user/email
     #(gen/fmap
       (fn [[user host domain]] (str user "@" host "." domain))
       (gen/tuple
        (gen/such-that non-blank-str? (gen/string-alphanumeric))
        (gen/such-that non-blank-str? (gen/string-alphanumeric))
        (gen/such-that non-blank-str? (gen/string-alphanumeric)))))))

(defn generate-data
  "Takes a hash-map of keys and specs and returns a hash-map of
   key and generated data based on the spec"
  [data]
  (into {}
        (map (fn [[key spec]]
               {key (gen/generate spec)}) data)))

(defn generate-user []
  (generate-data
   {:first-name (s/gen :receive.spec.user/first-name)
    :last-name  (s/gen :receive.spec.user/last-name)
    :email      email-gen}))

(defn generate-file []
  (generate-data
   {:filename (s/gen :receive.spec.file/filename)}))

(comment
  (generate-user)
  (generate-file))