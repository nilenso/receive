(defproject receive "0.1.0-SNAPSHOT"
  :description "Send and Receive files"
  :url "http://receive.nilenso.com"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/test.check "0.9.0"]

                 [ring "1.8.0"]
                 [bidi "2.1.6"]
                 [ring/ring-json "0.5.0"]
                 [ring-logger "1.0.1"]

                 [aero "1.1.6"]

                 [honeysql "0.9.10"]
                 [seancorfield/next.jdbc "1.0.409"]
                 [org.postgresql/postgresql "42.2.12"]
                 [hiccup "1.0.5"]
                 [ragtime "0.8.0"]

                 [clj-time "0.15.2"]
                 [org.clojure/data.json "1.0.0"]
                 [com.google.api-client/google-api-client "1.30.9"]
                 [buddy/buddy-sign "3.1.0"]
                 [camel-snake-kebab "0.4.1"]]
  :profiles {:test {:dependencies [[ring/ring-mock "0.4.0"]]}}
  :main receive.core
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler receive.core/app}
  :repl-options {:init-ns receive.core
                 :init (require '[clojure.repl :refer :all])}
  :aliases {"migrate"  ["run" "-m" "receive.db.migration/migrate"]
            "rollback" ["run" "-m" "receive.db.migration/rollback"]})
