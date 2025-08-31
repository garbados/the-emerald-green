(ns the-emerald-green.web.templates.guides
  (:require
   [the-emerald-green.web.alchemy :refer [profane]]
   ["marked" :as marked]
   [clojure.string :as string]
   [the-emerald-green.utils :refer-macros [inline-slurp]]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.deck :as deck]))

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
         [:li (string/join ", " (map deck/arcana-keyword->name subrule))]
         [:li (deck/arcana-keyword->name subrule)]))]))

(defn print-trait
  ([{trait-name :name
     requirements :requires
     :keys [description]}
    & [n]]
   [:div.card
    [:div.card-content>div.content
     [:p.subtitle trait-name (when (< 1 n) (str " (x " n ")"))]
     (profane "p" (marked/parse description))
     [:p "Requires:"]
     (print-reqs requirements)]]))

(def trait-guide
  [:div.content
   [:h1 "Trait Guide"]
   [:p "Here are documented all the fae traits you may... develop."]
   (map print-trait traits/all-traits)])

(def equipment-guide
  [:div.content
   [:h1 "Equipment Guide"]
   [:p "TODO"]])
