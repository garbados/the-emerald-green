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

(def enchantments (map idify (slurp-edn "resources/enchantments.edn")))
(def items (map idify (slurp-edn "resources/items.edn")))
(def *equipment (map idify (slurp-dir-edn "resources/equipment")))
(def *id->equipment (zipmap (map :id *equipment) *equipment))
(def equipment (map (partial refine-extensions *id->equipment) *equipment))
(def id->equipment (zipmap (map :id equipment) equipment))
(def type->stuff
  (merge
   {:items items}
   (group-by :type equipment)))

(def elements #{:physical :fire :frost :radiant :shadow})

(s/def ::name string?)
(s/def ::id keyword?)
(s/def ::description string?)

(s/def ::item
  (s/keys :req-un [::name
                   ::id
                   ::description]))

(s/def ::tag (set (flatten (map :tags equipment))))
(s/def ::tags (s/coll-of ::tag :kind set?))
(s/def ::extends (keys id->equipment))
(s/def ::type #{:weapon :armor :tool :consumable})
(def rarities #{:common :uncommon :rare :mythic})
(s/def ::rarity rarities)
(s/def ::base-equipment
  (s/keys :req-un [::name
                   ::description]
          :opt-un [::id
                   ::extends
                   ::type
                   ::enchantments
                   ::money/cost
                   ::rarity
                   ::tags]))

(s/def ::skill #{:melee :ranged :arcana :sorcery :theurgy})
(s/def ::heft #{:light :medium :heavy})
(s/def ::element elements)
(s/def ::range #{:close :short :medium :long :extreme})
(s/def ::weapon
  (s/and
   ::base-equipment
   (s/keys :opt-un [::skill
                    ::heft
                    ::element
                    ::range])))


(s/def ::resistances (s/map-of ::element int?))
(s/def ::inertia nat-int?)
(s/def ::armor
  (s/and
   ::base-equipment
   (s/keys :opt-un [::resistances
                    ::inertia])))

(s/def ::tool
  (s/and
   ::base-equipment
   (s/keys :opt-un [::core/skill])))

(s/def ::effect (s/or :basic string?
                      :ability ::core/ability))
(s/def ::consumable
  (s/and
   ::base-equipment
   (s/keys :opt-un [::effect])))

(s/def ::equipment*
  (s/or :weapon ::weapon
        :armor ::armor
        :tool ::tool
        :consumable ::consumable))

(s/def ::equipment (set equipment))
(s/def ::equipped (s/coll-of ::equipment))

(s/def ::item*
  (s/or
   :unequipped ::equipment
   :misc
   (s/keys :req-un [::name
                    ::description]
           :opt-un [::cost
                    ::rarity])))

(s/def ::item (set items))
(s/def ::inventory (s/coll-of ::item :kind vector?))
