(defproject talos "0.1.0"
  :description "talos"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.3.0"]
                 [clojurewerkz/elastisch "2.1.0-beta4"]
                 [ring/ring-core "1.3.1"]
                 [ring/ring-defaults "0.1.1"]
                 [ring/ring-json "0.3.1"]
                 [compojure "1.1.8"]
                 [http-kit "2.1.18"]
                 [clj-time "0.8.0"]
                 [cheshire "5.3.1"]
                 [org.yaml/snakeyaml "1.12"]
                 [clj-pid "0.1.1"]
                 [net.sf.trove4j/trove4j "3.0.3"]
                 [org.slf4j/slf4j-api "1.7.5"]
                 [org.slf4j/slf4j-log4j12 "1.7.5"]
                 [log4j/log4j "1.2.17"]
                 [com.esotericsoftware.kryo/kryo "2.22"]
                 [junit "4.11" :scope "test"]]

  :source-paths ["src/clj"]
  :java-source-paths ["src/java"]
  :test-paths ["test/clj"]
  :resource-paths ["resources"]

  :main talos.main
)
