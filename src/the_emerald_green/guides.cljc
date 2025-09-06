(ns the-emerald-green.guides
  (:require [the-emerald-green.macros :refer-macros [inline-slurp]]))

(def guides
  {:introduction  (inline-slurp "doc/introduction.md")
   :player-guide  (inline-slurp "doc/player_guide.md")
   :setting-guide (inline-slurp "doc/setting_guide.md")
   :gm-guide      (inline-slurp "doc/gm_guide.md")})
