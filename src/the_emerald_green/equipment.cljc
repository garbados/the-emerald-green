(ns the-emerald-green.equipment
  (:require
   [clojure.spec.alpha :as s]
   #?(:clj
      [the-emerald-green.utils :refer [slurp-edn]]
      :cljs
      [the-emerald-green.utils :refer-macros [slurp-edn]])))

(def enchantments (slurp-edn "resources/equipment/enchantments.edn"))
(def equipment
  (->>
   [(slurp-edn "resources/equipment/weapons.edn")
    (slurp-edn "resources/equipment/armor.edn")
    (slurp-edn "resources/equipment/tools.edn")
    (slurp-edn "resources/equipment/consumables.edn")]
   (reduce concat [])))

(def elements #{:physical :fire :frost :radiant :shadow})

(s/def ::name string?)
(s/def ::description string?)
(s/def ::type #{:weapon :armor :tool :consumable})

(s/def ::enchantments
  (s/coll-of
   (s/or :string string?
         :ref (keys enchantments))))
(s/def ::cost pos-int?)
(def rarities #{:common :uncommon :rare :mythic})
(s/def ::rarity rarities)
(s/def ::equipment*
  (s/keys :req-un [::name
                   ::description
                   ::type
                   ::enchantments
                   ::cost
                   ::rarity]))

(s/def ::skill #{:melee :ranged :arcana :sorcery :theurgy})
(s/def ::heft #{:light :medium :heavy})
(s/def ::element elements)
(s/def ::weapon
  (s/and
   ::equipment*
   (s/keys :req-un [::skill
                    ::heft
                    ::element
                    ::range])))

(s/def ::equipment*
  (s/or :weapon ::weapon
        :armor ::armor
        :tool ::tool
        :consumable ::consumable))

(s/def ::equipment (set equipment))
