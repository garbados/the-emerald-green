(ns the-emerald-green.core 
  (:require
   [clojure.spec.alpha :as s]))

(def attribute->skills
  {:body   [:resilience :athletics :intimidation :ranged :melee]
   :mind   [:insight :craft :medicine :diplomacy :arcana]
   :spirit [:resolve :awareness :deception :stealth :sorcery]
   :luck   [:gambling :appraisal :skepticism :divination :theurgy]})
(def attr-order [:body :mind :spirit :luck])
(def fung-order [:health :draw :will :fortune :madness])
(def attributes (set attr-order))
(def ordered-skills (reduce concat [] (vals attribute->skills)))
(def skills (into #{} ordered-skills))
(s/def ::attribute attributes)
(s/def ::skill skills)
(s/def ::attributes (s/map-of ::attribute nat-int?))
(s/def ::skills (s/map-of ::skill boolean?))

(def fungibles (set fung-order))
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
(s/def :ability/will nat-int?)
(s/def :ability/damage nat-int?)
(s/def :ability/madness nat-int?)
(s/def ::ability
  (s/keys :req-un [::name
                   ::description
                   :ability/phase
                   :ability/tags]
          :opt-un [:ability/madness
                   :ability/actions
                   :ability/will
                   :ability/damage]))
(s/def ::abilities (s/coll-of ::ability :kind set?))
