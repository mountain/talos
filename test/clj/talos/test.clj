(ns talos.test
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.query :as q]
            [clojurewerkz.elastisch.rest.response :as esrsp]
            [clojure.pprint :as pp]))

(def test-mapping-types
  {"discussion"
   {:properties
    {:author
     {:type "string"}
     :timestamp
     {:type "date"}
     :length
     {:type "integer"}
     :text
     {:type "string"}}}
   "discussion-by-hour"
   {:properties
    {:timestamp
     {:type "date"}
     :text
     {:type "string"}}}})

(defn tidy-raw [date]
  (fn [raw]
    (let [[meta text] raw
          [timestamp author] (clojure.string/split meta #"\s")
          timestamp (str date "T" timestamp "+08")]
      {:timestamp timestamp
       :author    author
       :length    (alength (.getBytes text))
       :text      text})))

(defn analyze-page [date]
  (let [html (slurp (str "docs/" date ".html"))
        doc (org.jsoup.Jsoup/parse html)
        ps (vec (.select doc "p"))
        sz (count ps)
        data (map (tidy-raw date)
                  (partition 2 (interleave
                                 (map #(.text (get ps %)) (range 1 sz 4))
                                 (map #(.text (get ps %)) (range 4 sz 4)))))]
    data))

(defn group-page-by-hour [page]
  (reduce #(merge-with str %1 %2) {}
          (map #(assoc {}
                 (first (clojure.string/split (:timestamp %1) #":"))
                 (:text %1)) page)))

(defn init-es []
  (let [conn (esr/connect "http://127.0.0.1:9931")]
    (try
      (println (esi/create conn "khex" :mappings test-mapping-types))
      (catch Throwable e (println e)))))

(defn import-es [page]
  (let [conn (esr/connect "http://127.0.0.1:9931")]
    (try
      (doseq [d page]
        (println (esd/create conn "khex" "discussion" d)))
      (catch Throwable e (println e)))
    (doseq [[k v] (group-page-by-hour page)]
      (try
        (println (esd/create conn "khex" "discussion-by-hour" {:timestamp k :text v}))
        (catch Throwable e (println e))))))

(defn import-page [date]
  (import-es (analyze-page date)))

