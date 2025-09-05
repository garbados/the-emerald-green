(ns the-emerald-green.web.templates.guides
  (:require
   ["marked" :as marked]
   [the-emerald-green.macros :refer-macros [inline-slurp]]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.web.alchemy :refer [profane]]
   [the-emerald-green.web.templates.traits :refer [print-trait]]))

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

(def trait-guide
  [:div.content
   [:h1 "Trait Guide"]
   [:p "Here are documented all the fae traits you may... develop."]
   (map print-trait traits/traits)])

(def equipment-guide
  [:div.content
   [:h1 "Equipment Guide"]
   [:p "TODO"]])
