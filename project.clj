(defproject receive "0.1.0-SNAPSHOT"
  :description "Send and Receive files"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [ring "1.8.0"]
                 [bidi "2.1.6"]
                 [ring/ring-json "0.5.0"]
                 [ring-logger "1.0.1"]]
  :profiles {:test {:dependencies [[ring/ring-mock "0.4.0"]]}}
  :main receive.core
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler receive.core/app}
  :repl-options {:init-ns receive.core})
