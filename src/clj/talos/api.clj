(ns talos.api
  (:use [clojurewerkz.elastisch.rest :only [connect]]
        [compojure.core :only [defroutes GET POST PUT DELETE ANY context]]
        [ring.util.response :only [response]]
        [talos.meta :only [add-doc]]
        [cheshire.core :only [parse-string]]
        [clojure.tools.logging :as log]))

(def conn (connect "http://127.0.0.1:9929"))

(defn- slurp-binary
  "Reads len bytes from InputStream is and returns a byte array."
  [^java.io.InputStream is len flag]
  (with-open [rdr is]
    (let [buf (byte-array len)]
      (if flag (.reset is))
      (.read rdr buf)
      buf)))

(defroutes apiroutes
  (GET "/:index/_train" [index]
    (fn [req]
      (response {:message "OK"})))
  (GET "/:index/_index" [index]
    (fn [req]
      (response {:message "OK"})))
  (POST "/:index/:type" [index type]
    (fn [req]
      (let [body (if-let [len (get-in req [:headers "content-length"])]
                   (slurp-binary (:body req) (Integer/parseInt len) true))
            req-body (parse-string (String. body (java.nio.charset.Charset/forName "UTF-8")))
            body (if-let [len (get-in (:es-result req) [:headers "content-length"])]
                   (slurp-binary (:body (:es-result req)) (Integer/parseInt len) false))
            resp-body (parse-string (String. body (java.nio.charset.Charset/forName "UTF-8")))]

        (add-doc conn index type (get resp-body "_id") req-body)

        (response {:message "OK"}))))
  (POST "/:index/:type/:id" [index type id]
    (fn [req]
      (let [body (if-let [len (get-in req [:headers "content-length"])]
                   (slurp-binary (:body req) (Integer/parseInt len) true))
            bstr (String. body (java.nio.charset.Charset/forName "UTF-8"))
            args (parse-string bstr)]
        (add-doc conn index type id args)
        (response {:message "OK"}))))
  (PUT "/:index/:type/:id" [index type id]
    (fn [req]
      (let [body (if-let [len (get-in req [:headers "content-length"])]
                   (slurp-binary (:body req) (Integer/parseInt len) true))
            bstr (String. body (java.nio.charset.Charset/forName "UTF-8"))
            args (parse-string bstr)]
        (add-doc conn index type id args)
        (response {:message "OK"}))))
  (GET "/:index/:type/:id/_rec" [index type id]
    (fn [req]
      (response {:message "OK"}))))
