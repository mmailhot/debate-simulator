(ns debatesim.utils.generate
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.edn :as edn]))

(def data
  {
   "harper" (edn/read-string (-> "edn/harper.edn" io/resource io/file slurp))
   "trudeau" (edn/read-string (-> "edn/trudeau.edn" io/resource io/file slurp))
   "mulcair" (edn/read-string (-> "edn/mulcair.edn" io/resource io/file slurp))
   "may" (edn/read-string (-> "edn/may.edn" io/resource io/file slurp))
   "moderator" (edn/read-string (-> "edn/moderator.edn" io/resource io/file slurp))
   }
 )

(def SINGLE_LOOKBACK_RATE 0.10)

(defn get-word [d second-last last]
  (let [ks (keys (get d (str second-last " " last)))
        to-select (loop [ks ks
                         built-list []]
                    (if (= (count ks) 0)
                      built-list
                      (recur (rest ks)
                             (concat (repeat (get (get d (str second-last " " last)) (first ks)) (first ks)) built-list))))]
    (rand-nth to-select)
    ))

(defn get-next [speaker second-last last]
  (if (< (rand) SINGLE_LOOKBACK_RATE)
    (get-word (get data speaker) "*" last)
    (get-word (get data speaker) second-last last)))

(defn generate-markov [speaker]
  (str/join
   " "
   (reverse (drop 1 (reverse (drop 2
         (loop [chain ["*START*" "*START*"]]
           (if (= (last chain) "*END*")
             chain
             (recur (conj chain (get-next speaker (nth chain (- (count chain) 2)) (nth chain (- (count chain) 1)))))))))))))
