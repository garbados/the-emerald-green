(ns the-emerald-green.web.utils 
  (:require
   [the-emerald-green.web.alchemy :refer [alchemize snag]]))

(defn refresh-node [node-id template-fn]
  (.replaceChildren (snag node-id)
                    (alchemize (template-fn))))

(defn static-view [template]
  (fn [node _hash]
    (.replaceChildren node
                      (alchemize template))))
