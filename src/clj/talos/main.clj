(ns talos.main
  (:use ring.middleware.defaults
        [ring.middleware.json :only [wrap-json-response]]
        [clojure.tools.logging :as log]
        org.httpkit.server
        talos.api
        talos.es
        talos.meta)
  (:gen-class))

(defn api [req]
  (try
    (let [es-result (proxy-es req)
          req (assoc req :es-result es-result)
          api-result
          ((wrap-defaults (wrap-json-response apiroutes) api-defaults) req)]
      (if (nil? es-result) api-result es-result))
    (catch Throwable e (log/error e))))

(defn -main [& args]
  (start-es)
  (init-meta)
  (run-server api {:ip "127.0.0.1" :port 9931}))

