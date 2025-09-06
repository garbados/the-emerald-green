(ns the-emerald-green.web.utils 
  (:require
   [the-emerald-green.web.alchemy :refer [alchemize refresh]]))

(def refresh-node refresh)

(defn static-view [template]
  (fn [node _hash]
    (.replaceChildren node
                      (alchemize template))))
