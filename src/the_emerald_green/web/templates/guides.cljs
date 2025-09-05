(ns the-emerald-green.web.templates.guides
  (:require
   ["marked" :as marked]
   [the-emerald-green.macros :refer-macros [inline-slurp]]
   [the-emerald-green.web.alchemy :refer [profane]]))

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
