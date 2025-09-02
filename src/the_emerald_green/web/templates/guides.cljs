(ns the-emerald-green.web.templates.guides
  (:require
   ["marked" :as marked]
   [clojure.string :as string]
   [the-emerald-green.macros :refer-macros [inline-slurp]]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.utils :as utils]
   [the-emerald-green.web.alchemy :refer [profane]]))

;; STATIC GUIDES

(defn guide-text [text]
  [:div.content
   (profane "span" (marked/parse text))])

(let [text (inline-slurp "doc/introduction.md")]
  (def introduction (guide-text text)))

(let [text (inline-slurp "doc/player_guide.md")]
  (def player-guide (guide-text text)))

(let [text (inline-slurp "doc/setting_guide.md")]
  (def setting-guide (guide-text text)))

(let [text (inline-slurp "doc/gm_guide.md")]
  (def gm-guide (guide-text text)))

;; DYNAMIC GUIDES

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

(def trait-guide
  [:div.content
   [:h1 "Trait Guide"]
   [:p "Here are documented all the fae traits you may... develop."]
   (map print-trait traits/traits)])

(def equipment-guide
  [:div.content
   [:h1 "Equipment Guide"]
   [:p "TODO"]])
