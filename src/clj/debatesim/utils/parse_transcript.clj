(ns debatesim.utils.parse-transcript
  (:require [hickory.core :as h]
            [hickory.select :as s]
            [clj-http.lite.client :as http]
            [clojure.string :as str]
            [markov.core :as markov]
            [clojure.java.io :as io]
            )
  )

(def harper (atom []))
(def trudeau (atom []))
(def mulcair (atom []))
(def may (atom []))
(def duceppe (atom []))
(def moderator (atom []))

(defn find-children [type el]
  (filter #(if (or (seq? %) (vector? %))
               (= (first %) type)
               false) el))

(defn filter-by-class [class els]
  (filter
   (fn [x]
     (let [class (->> x
                      second
                      :class
                      )]
       (if class
         (do
           (println class)
           (->> (str/split class #" ")
                (some #(= class %))))
         false)))
   els))

(defn filter-by-id [id els]
  (filter
   (fn [x]
     (->> x
          second
          :id
          (= id)))
   els))

(defn add-text [textel]
  (when (= (count textel) 2)
    (when (= (:tag (first textel)) :strong)
      (let [speaker (str/lower-case (first (:content (first textel))))
            text (if (string? (second textel))
                   (second textel)
                   (first (:content (second textel))))]
        (cond
          (.contains speaker "harper") (swap! harper #(conj % text))
          (.contains speaker "trudeau") (swap! trudeau #(conj % text))
          (.contains speaker "mulcair") (swap! mulcair #(conj % text))
          (.contains speaker "may") (swap! may #(conj % text))
          (.contains speaker "duceppe") (swap! duceppe #(conj % text))
          :else (swap! moderator #(conj % text)))
        nil))))

(defn parse-site [url]
  (->> url
       http/get
       :body
       h/parse
       h/as-hickory
       (s/select (s/child (s/id :main)))
       first
       (s/select (s/child (s/class "entry-content")
                          (s/tag :p)))
       (map :content)
       (map add-text)
       doall))

(defn post-process [l]
  (reduce
   (fn [t1 t2]
     (let [trimmed1 (.replaceAll t1 "(^\\h*)|(\\h*$)" "")
           trimmed2 (.replaceAll t2 "(^\\h*)|(\\h*$)" "")]
       (if (and (or (.endsWith trimmed1 "–")
                    (.endsWith trimmed1 "-")
                    (.endsWith trimmed1 "—"))
                (or (.startsWith trimmed2 "–")
                    (.startsWith trimmed2 "-")
                    (.startsWith trimmed2 "—")))
         (str (.substring trimmed1 0 (- (.length trimmed1) 2)) " " (.substring trimmed2 2 (.length trimmed2)))
         (str trimmed1 "*PEND*" trimmed2))))
   l))

(defn add-to [atom x y z]
  (if (and (contains? @atom (str x " " y))
           (contains? (get @atom (str x " " y)) z))
    (swap! atom #(assoc-in % [(str x " " y) z] (+ (get (get % (str x " " y)) z) 1)))
    (swap! atom #(assoc-in % [(str x " " y) z] 1))))

(defn generate-chain-data [text]
  (let
    [split-text (str/split text #"\*PEND\*")
     mdata (atom {})]
    (doall
     (map
      (fn [sentence]
        (let [words (concat ["*START*" "*START*"] (str/split sentence #"\h+") ["*END*"])]
          (println (into [] words))
          (doall (map
                  (fn [x]
                    (add-to mdata (first x) (second x) (nth x 2))
                    (add-to mdata "*" (second x) (nth x 2)))
                  (partition 3 1 words)))))
      split-text))
    @mdata))

(defn dump-transit [filename data]
  (with-open [wrtr (io/writer filename)]
    (.write wrtr (prn-str data))))

(defn build-corpus []
  (doall
   (map parse-site
        ["http://www.macleans.ca/politics/ottawa/tale-of-the-tape-read-a-full-transcript-of-macleans-debate/"
         "http://www.macleans.ca/politics/ottawa/tale-of-the-tape-transcript-of-the-munk-debate-on-the-refugee-crisis/"
         "http://www.macleans.ca/politics/ottawa/tale-of-the-tape-transcript-of-the-globe-debate-on-the-economy/"]))
  (doall
   (map #(swap! % post-process)
        [harper trudeau mulcair may moderator]))
  (doall
   (map #(swap! % generate-chain-data)
        [harper trudeau mulcair may moderator]))
  (dump-transit "harper.edn" @harper)
  (dump-transit "trudeau.edn" @trudeau)
  (dump-transit "mulcair.edn" @mulcair)
  (dump-transit "may.edn" @may)
  (dump-transit "moderator.edn" @moderator))
