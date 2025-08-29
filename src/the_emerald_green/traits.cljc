(ns the-emerald-green.traits
  (:require
   [clojure.set :as set]
   [clojure.spec.alpha :as s]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.utils :refer [slurp-edn]]))

(def all-traits
  (->>
   [(slurp-edn "resources/traits/attributes.edn")
    (slurp-edn "resources/traits/skills.edn")]
   (reduce concat [])))

(s/def ::name string?)
(s/def ::description string?)

(s/def ::requires**
  (s/or
   :solo ::deck/tag
   :coll (s/coll-of ::deck/tag :kind vector?)))
(s/def ::requires*
  (s/or :and-or
        (s/cat :pred #{:or :and}
               :expr (s/+ ::requires**))
        :count
        (s/cat :pred #{:count}
               :n pos-int?
               :expr (s/+ ::requires**))
        :expr ::requires**))
(s/def ::requires (set (map :requires all-traits)))

(s/def ::attributes (s/map-of keyword? integer?))
(s/def ::skills (s/map-of keyword? true?))
(s/def ::unique true?)
(s/def ::effect
  (s/keys :opt-un [::attributes
                   ::skills]))
(s/def ::trait*
  (s/keys :req-un [::name
                   ::description
                   ::requires]
          :opt-un [::unique
                   ::effect]))

(s/def ::trait (set all-traits))

(defn rule-matches-card? [rule {tags :tags :as card}]
  (cond
    (keyword? rule)
    (contains? (:tags card) rule)
    (= :or (first rule))
    (true? (some #(rule-matches-card? % card) (rest rule)))
    (= :and (first rule))
    (every? #(rule-matches-card? % card) (rest rule))
    (seq rule)
    (empty? (set/difference (set rule) tags))))

(s/fdef rule-matches-card?
  :args (s/cat :rule ::requires
               :card ::deck/card)
  :ret boolean?)

(defn rule-matches-deck? [rule cards]
  (cond
    (and (sequential? rule) (= :and (first rule)))
    (every? #(rule-matches-deck? % cards) (rest rule))
    (and (sequential? rule) (= :count (first rule)))
    (>= (second rule)
        (count (filter #(rule-matches-card? (nth rule 2) %) cards)))
    :else
    (> 0 (count (filter #(rule-matches-card? rule %) cards)))))

(s/fdef rule-matches-deck?
  :args (s/cat :rule ::requires
               :card ::deck/deck)
  :ret boolean?)
