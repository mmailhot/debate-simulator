(ns debatesim.test-runner
  (:require
   [cljs.test :refer-macros [run-tests]]
   [debatesim.core-test]))

(enable-console-print!)

(defn runner []
  (if (cljs.test/successful?
       (run-tests
        'debatesim.core-test))
    0
    1))
