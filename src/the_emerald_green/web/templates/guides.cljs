(ns the-emerald-green.web.templates.guides
  (:require ["marked" :as marked]
            [the-emerald-green.web.alchemy :refer [profane]]
            [the-emerald-green.guides :as guides]))

(defn guide-text [text]
  [:div.content
   (profane "span" (marked/parse text))])

(def guides
  (reduce
   (fn [acc [key text]] (assoc acc key (guide-text text)))
   {}
   guides/guides))
