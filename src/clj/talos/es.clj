(ns talos.es
  (:require [clojure.string :refer [join split]]
            [clj-http.client :refer [request]])
  (:import (java.net URI)))

(defn start-es []
  (System/setProperty "es.path.home"
                      (str (System/getProperty "user.home")
                           (System/getProperty "file.separator")
                           ".talos"))
  (System/setProperty "es.network.host" "127.0.0.1")
  (System/setProperty "es.http.port" "9929")
  (System/setProperty "es.cluster.name" "talos")
  (System/setProperty "es.index.number_of_shards" "1")
  (System/setProperty "es.index.number_of_replicas" "0")
  (org.elasticsearch.bootstrap.Elasticsearch/main (into-array String [])))

(defn- slurp-binary
  "Reads len bytes from InputStream is and returns a byte array."
  [^java.io.InputStream is len]
  (with-open [rdr is]
    (let [buf (byte-array len)]
      (.mark is len)
      (.read rdr buf)
      buf)))

(defn proxy-es [req]
  (let [uri (URI. "http://127.0.0.1:9929")
        remote-uri (URI. (.getScheme uri)
                         (.getAuthority uri)
                         (:uri req)
                         nil
                         nil)
        remote-result (request
                        {:method           (:request-method req)
                         :url              (str remote-uri "?" (:query-string req))
                         :headers          (dissoc (:headers req) "host" "content-length")
                         :body             (if-let [len (get-in req [:headers "content-length"])]
                                             (slurp-binary (:body req) (Integer/parseInt len)))
                         :follow-redirects true
                         :throw-exceptions false
                         :as               :stream})]
    (if (== (:status remote-result) 400) nil remote-result)))
