(ns the-emerald-green.web.alchemy
  (:require ["html-alchemist" :as alchemy]))

(def snag alchemy/snag)
(def profane alchemy/profane)

(defn alchemize
  "Clojure-friendly wrapper around Alchemist's alchemize function."
  [expr]
  (println expr)
  (alchemy/alchemize (clj->js expr)))
