(ns the-emerald-green.character 
  (:require
   [clojure.spec.alpha :as s]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.equipment :as equipment]))

(def attribute->skills
  {:body   [:athletics :melee :ranged :resilience :intimidation]
   :mind   [:arcana :craft :diplomacy :insight :medicine]
   :spirit [:awareness :deception :sorcery :stealth :resolve]
   :luck   [:theurgy]})
(def attributes (set (keys attribute->skills)))
(def skills (reduce into #{} (vals attribute->skills)))

(s/def ::name string?)
(s/def ::biography string?)
(s/def ::sanctified (s/coll-of ::deck/card :distinct true :count 2))
(s/def ::absences (s/coll-of ::deck/card :distinct true :min-count 3))
(s/def ::wealth nat-int?)

;; a version of a character that can be safely persisted to disk
(s/def ::persistent-character
  (s/keys :req-un [::name
                   ::biography
                   ::sanctified
                   ::absences
                   ::equipment/equipment
                   ::wealth]))

(s/def ::attributes (s/map-of attributes pos-int?))
(s/def ::skills (s/map-of skills boolean?))
(s/def ::talent
  (s/keys :req-un [::name
                   ::description]))
(s/def ::talents (s/coll-of ::talent))
(s/def ::ability
  (s/keys :req-un [::name
                   ::description
                   ::actions]))
(s/def ::abilities (s/coll-of ::ability))

;; "hydrated" character datastructure
(s/def ::character
  (s/and
   ::persistent-character
   (s/keys :req-un [::attributes
                    ::skills
                    ::talents
                    ::abilities])))

(defn hydrate-character [persistent-character]
  'todo)

(s/fdef hydrate-character
  :args (s/cat :persistent-character ::persistent-character)
  :ret ::character)
