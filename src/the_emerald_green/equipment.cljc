(ns the-emerald-green.equipment
  (:require
   #?(:clj
      [the-emerald-green.macros :refer [slurp-dir-edn slurp-edn]]
      :cljs
      [the-emerald-green.macros :refer-macros [slurp-edn slurp-dir-edn]])
   [clojure.spec.alpha :as s]
   [the-emerald-green.core :as core]
   [the-emerald-green.money :as money]
   [the-emerald-green.utils :as utils]))

(def enchantments (slurp-edn "resources/enchantments.edn"))
(def items (slurp-edn "resources/items.edn"))
(def equipment (map utils/idify (slurp-dir-edn "resources/equipment")))

(def elements #{:physical :fire :frost :radiant :shadow})

(s/def ::name string?)
(s/def ::id keyword?)
(s/def ::description string?)
(s/def ::tag keyword?) ; FIXME actual set
(s/def ::tags (s/coll-of ::tag :kind set?))
(s/def ::extends ::id)
(s/def ::type #{:weapon :armor :tool :consumable})
(s/def ::enchantments
  (s/coll-of
   (s/or :string string?
         :ref (keys enchantments))))
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
