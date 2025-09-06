(ns the-emerald-green.core 
  (:require
   [clojure.spec.alpha :as s]))

(def attribute->skills
  {:body   [:athletics :melee :ranged :resilience :intimidation]
   :mind   [:arcana :craft :diplomacy :insight :medicine]
   :spirit [:awareness :deception :sorcery :stealth :resolve]
   :luck   [:theurgy]})
(def attributes (set (keys attribute->skills)))
(def skills (reduce into #{} (vals attribute->skills)))
(s/def ::attribute attributes)
(s/def ::skill skills)
(s/def ::attributes (s/map-of ::attribute nat-int?))
(s/def ::skills (s/map-of ::skill boolean?))

(def fungibles #{:health :draw :will :fortune :madness})
(s/def ::fungible fungibles)
(s/def ::fungibles (s/map-of ::fungible nat-int?))

(s/def ::name string?)
(s/def ::description string?)
(s/def ::talent
  (s/keys :req-un [::name
                   ::description]))
(s/def ::talents (s/coll-of ::talent))
(s/def :ability/actions (s/int-in 0 4))
(s/def :ability/phase #{:encounter :exploration :downtime})
(s/def :ability/tags (s/coll-of keyword?))
(s/def ::ability
  (s/keys :req-un [::name
                   ::description
                   :ability/actions
                   :ability/phase]
          :opt-un [:ability/tags]))
(s/def ::abilities (s/coll-of ::ability :kind set?))
