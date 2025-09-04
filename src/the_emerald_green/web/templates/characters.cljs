(ns the-emerald-green.web.templates.characters 
  (:require
   ["marked" :as marked]
   [clojure.string :as string]
   [the-emerald-green.utils :as utils]
   [the-emerald-green.web.alchemy :refer [profane]]))

(defn print-reqs [rule]
  (cond
    (#{:and :or} (first rule))
    [:ul>li
     [:p (string/capitalize (name (first rule)))]
     (print-reqs (rest rule))]
    (= :count (first rule))
    [:ul>li
     [:p (str "Needs " (second rule))]
     (print-reqs (drop 2 rule))]
    :else
    [:ul
     (for [subrule rule]
       (if (sequential? subrule)
         [:li (string/join ", " (map utils/keyword->name subrule))]
         [:li (utils/keyword->name subrule)]))]))

(defn print-trait
  ([{trait-name :name
     trait-reqs :traits
     deck-reqs :deck
     card-reqs :card
     :keys [description]}
    & [n]]
   [:div.card
    [:div.card-content>div.content
     [:p.subtitle trait-name (when (< 1 n) (str " (x " n ")"))]
     (profane "p" (marked/parse description))
     [:p "Requires:"]
     [:ul
      (for [[reqs title] [[trait-reqs "Traits"]
                          [deck-reqs "Deck"]
                          [card-reqs "Card"]]
            :when reqs]
        [:li
         [:div.level
          [:div.level-item [:p title]]
          [:div.level-item [:pre>code (print-str reqs)]]]])]]]))
