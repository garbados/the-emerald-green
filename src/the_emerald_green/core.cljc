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
(s/def ::attributes (s/map-of ::attribute integer?))
(s/def ::skills (s/map-of ::skill true?))

(def fungibles #{:health :draw :will :fortune :madness})
(s/def ::fungible fungibles)
(s/def ::fungibles (s/map-of ::fungible nat-int?))

(s/def ::talent
  (s/keys :req-un [::name
                   ::description]))
(s/def ::talents (s/coll-of ::talent))
(s/def ::ability
  (s/keys :req-un [::name
                   ::description
                   ::actions]))
(s/def ::abilities (s/coll-of ::ability :kind set?))
