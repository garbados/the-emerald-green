(ns the-emerald-green.equipment
  (:require
   #?(:clj
      [the-emerald-green.macros :refer [slurp-dir-edn slurp-edn]]
      :cljs
      [the-emerald-green.macros :refer-macros [slurp-edn slurp-dir-edn]])
   [clojure.spec.alpha :as s]
   [the-emerald-green.core :as core]
   [the-emerald-green.money :as money]
   [the-emerald-green.utils :refer [idify refine-extensions]]))

(def enchantments (map idify (slurp-edn "enchantments.edn")))
(def *equipment (map idify (slurp-dir-edn "equipment")))
(def *id->equipment (zipmap (map :id *equipment) *equipment))
(def equipment (map (partial refine-extensions *id->equipment) *equipment))
(def id->equipment (zipmap (map :id equipment) equipment))
(def type->stuff (group-by :type equipment))
(def equippable-types #{:weapon :armor :tool :consumable})
(def equippable (reduce concat [] (vals (select-keys type->stuff equippable-types))))

(def elements #{:physical :fire :frost :radiant :shadow})

(s/def ::name string?)
(s/def ::id keyword?)
(s/def ::description string?)
(s/def ::content-pack string?)
(def known-tags (set (reduce into [] (filter some? (map :tags equipment)))))
(s/def ::tag known-tags)
(s/def ::tags (s/coll-of ::tag :kind set?))
(s/def ::extends (set (keys id->equipment)))
(def rarities #{:common :uncommon :rare :mythic})
(s/def ::rarity rarities)
(s/def :equippable/type equippable-types)
(s/def ::type (conj equippable-types :item))

(s/def ::item
  (s/keys :req-un [::name
                   ::id
                   ::type
                   ::description]
          :opt-un [::content-pack
                   ::extends
                   ::enchantments
                   ::money/cost
                   ::rarity
                   ::tags]))

(def weapon-skills #{:melee :ranged :arcana :sorcery :theurgy})
(s/def ::skill weapon-skills)
(def hefts #{:light :medium :heavy})
(s/def ::heft hefts)
(s/def ::element elements)
(def weapon-ranges #{:close :short :medium :long :extreme})
(s/def ::range weapon-ranges)
(s/def ::weapon
  (s/merge
   ::item
   (s/keys :opt-un [::skill
                    ::heft
                    ::element
                    ::range])))


(s/def ::resistances (s/map-of ::element int?))
(s/def ::inertia nat-int?)
(s/def ::armor
  (s/merge
   ::item
   (s/keys :opt-un [::resistances
                    ::inertia])))

(s/def ::tool
  (s/merge
   ::item
   (s/keys :opt-un [::core/skill])))

(s/def ::effect (s/or :basic string?
                      :ability ::core/ability))
(s/def ::consumable
  (s/merge
   ::item
   (s/keys :opt-un [::effect])))

(s/def ::equipment*
  (s/merge
   ::item
   (s/keys :req-un [:equippable/type])))

(s/def ::equipment (set (keys id->equipment)))
(s/def ::equipped (s/coll-of ::equipment))
(s/def ::inventory (s/coll-of ::item))
