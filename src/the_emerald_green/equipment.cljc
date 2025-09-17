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
(def id->enchantment (zipmap (map :id enchantments) enchantments))
(def *equipment (map idify (slurp-dir-edn "equipment")))
(def *id->equipment (zipmap (map :id *equipment) *equipment))
(def equipment (map (partial refine-extensions *id->equipment) *equipment))
(def id->equipment (zipmap (map :id equipment) equipment))
(def type->stuff (group-by :type equipment))
(def stuff-types [:weapon :armor :tool :consumable :item])
(def equippable-types #{:weapon :armor :tool :consumable})
(def equippable (reduce concat [] (vals (select-keys type->stuff equippable-types))))
(def all-tags (set (reduce concat [] (map :tags equipment))))

(def elements [:physical :fire :frost :radiant :shadow])

(s/def ::name string?)
(s/def ::id keyword?)
(s/def ::_id string?)
(s/def ::description string?)
(s/def ::content-pack keyword?)
(s/def ::enchantments
  (s/coll-of
   (s/or :ref keyword?
         :def (s/keys :req-un [::name
                               ::id
                               ::description
                               ::content-pack]))))
(def known-tags (set (reduce into [] (filter some? (map :tags equipment)))))
(s/def ::tag known-tags)
(s/def ::tags (s/coll-of ::tag :kind set?))
(s/def ::extends (set (keys id->equipment)))
(def rarities [:common :uncommon :rare :mythic])
(s/def ::rarity (set rarities))
(s/def :equippable/type equippable-types)
(s/def ::type (conj equippable-types :item))

(s/def ::item
  (s/keys :req-un [::name
                   ::type
                   ::description
                   ::content-pack]
          :opt-un [::extends
                   ::enchantments
                   ::money/cost
                   ::rarity
                   ::tags
                   ::id
                   ::_id]))

(def weapon-skills [:melee :ranged :arcana :sorcery :theurgy])
(s/def ::skill (set weapon-skills))
(def weapon-hefts [:light :medium :heavy])
(s/def ::heft (set weapon-hefts))
(s/def ::element (set elements))
(def weapon-ranges [:close :near :distant :far :extreme])
(s/def ::range (set weapon-ranges))
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

(def base-props [:cost :rarity :content-pack :enchantments :tags])
(def weapon-props [:heft :range :element])
(def armor-props [:resistances :inertia])
(def tool-props [:skill])
(def consumable-props [:effect])

(def type->props
  {:weapon (concat base-props weapon-props)
   :armor (concat base-props armor-props)
   :tool (concat base-props tool-props)
   :consumable (concat base-props consumable-props)
   :item base-props})

(def type->title
  {:weapon "Weapons"
   :armor "Armor"
   :tool "Tools"
   :consumable "Consumables"
   :item "Items"})

(s/def ::equipment (set (keys id->equipment)))
(s/def ::equipped (s/coll-of ::equipment*))

(defn ^:no-stest merge-custom-stuff [custom-stuff]
  (merge-with concat type->stuff (group-by :type (vals custom-stuff))))
