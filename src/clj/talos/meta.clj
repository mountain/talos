(ns talos.meta
  (:require [clojurewerkz.elastisch.rest :as esr]
            [clojurewerkz.elastisch.rest.index :as esi]
            [clojurewerkz.elastisch.rest.bulk :as esb]
            [clojurewerkz.elastisch.rest.document :as esd]
            [clojurewerkz.elastisch.query :as q]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [clj-time.core :as tc]
            [clj-time.coerce :as td]
            [cheshire.core :as json]
            [clojure.tools.logging :as log]))

(def id2wd (atom {}))
(def wd2id (atom {}))
(def bulk-list (atom []))
(def stopword
  (with-open [rdr (clojure.java.io/reader (io/file (io/resource "stopword.txt")))]
    (set (map #(string/trim %) (line-seq rdr)))))

(def config (org.talos.JcsegTaskConfig.))
(def dictionary (org.talos.voc.DictionaryFactory/createDefaultDictionary config))

(def mapping-types
  {"vocabulary"
   {:properties
    {:timestamp
     {:type "date"}
     :word
     {:type "string" :index "not_analyzed"}}}
   "documents"
   {:properties
    {:index
     {:type "string" :index "not_analyzed"}
     :type
     {:type "string" :index "not_analyzed"}
     :id
     {:type "string" :index "not_analyzed"}
     :timestamp
     {:type "date"}
     :model
     {:type "date"}
     :words
     {:type "object"}}}})

(defn not-stopword [wd]
  (not (contains? stopword wd)))

(defn to-freq [wdseq]
  (let [freq (atom {})]
    (doall
      (doseq [wd wdseq]
        (let [cnt (get @freq wd)
              cnt (if (nil? cnt) 1 (inc cnt))]
          (swap! freq assoc wd cnt))))
    @freq))

(defn add-word [conn wd]
  (let [id (get @wd2id wd)]
    (if (nil? id)
      (let [nid (inc (count @wd2id))]
        (swap! id2wd assoc nid wd)
        (swap! wd2id assoc wd nid)

        (swap! bulk-list conj {:index {:_index "talos" :_type "vocabulary" :_id (format "%010d" nid)}})
        (swap! bulk-list conj {:word wd :timestamp (td/to-long (tc/now))})
        (if (== (mod nid 1000) 0)
          (do
            (esb/bulk conn @bulk-list)
            (swap! bulk-list empty)))))
    wd))

(defn segment [conn text]
  (let [segmenter (org.talos.nlp.Segmenter. config dictionary)
        result (atom [])]
    (.reset segmenter text)
    (loop [wd (.next segmenter)]
      (when (not (nil? wd))
        (swap! result conj (add-word conn (.value wd)))
        (recur (.next segmenter))))
    (filter not-stopword @result)))

(defn add-doc [conn index type docid doc]
  (let [ndoc (hash-map :index index :type type :id docid :timestamp (td/to-long (tc/now))
                       :words (to-freq (segment conn (get doc "text"))))]
    (esd/create conn "talos" "documents" ndoc :id (str index "-" type "-" docid))))

(defn init-meta []
  (let [conn (esr/connect "http://127.0.0.1:9929")]
    (try
      (esi/create conn "talos" :mappings mapping-types)
      (with-open [rdr (clojure.java.io/reader (io/file (io/resource "voc-base.txt")))]
        (doall (map #(add-word conn %)
                    (map #(string/trim (get (string/split % #"\s") 1)) (line-seq rdr)))))
      (esb/bulk conn @bulk-list)
      (swap! bulk-list empty)
      (catch Throwable e (log/error e)))))
