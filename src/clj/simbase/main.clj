(ns simbase.main
  (:require [clj-pid.core :as pid])
  (:gen-class :main true))

(def logger (org.slf4j.LoggerFactory/getLogger (class org.talos.vec.Base)))

(defn- load-config []
  (try
    (let [yaml (org.yaml.snakeyaml.Yaml.)]
      (.load yaml (java.io.FileReader. "config/simbase.yaml")))
    (catch java.io.IOException e
      (.warn logger "config file simbase.yaml was not found, loading the default config"))))

(defn -main [& args]
  (let [config (merge {"server" {"ip" "0.0.0.0" "port" 7654} "pidfile" "log/pid"} (load-config))
        pid-file (get config "pidfile")
        port (get config "server" "port")]
    (println "------------------------------------------------")
    (println "base[" (pid/current) "] started on port" port)
    (println "------------------------------------------------")
    (pid/save pid-file)
    (pid/delete-on-shutdown! pid-file)

    (let [context (org.talos.vec.Config. config)
          database (org.talos.vec.Base. context)]
      (try
        (.run database)
        (catch java.lang.Throwable e (do
                                       (.error logger "Server Error!" e)
                                       (System/exit -1)))))))

