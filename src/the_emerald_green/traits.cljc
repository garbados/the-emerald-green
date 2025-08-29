(ns the-emerald-green.traits
  (:require
   [clojure.set :as set]
   [clojure.spec.alpha :as s]
   [the-emerald-green.character :as core]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.utils :refer-macros [slurp-edn]]))

(def all-traits
  (->>
   [(slurp-edn "resources/traits/attributes.edn")]
   (reduce concat [])))

(s/def ::name string?)
(s/def ::description string?)

(s/def ::requires* (s/coll-of ::deck/tag :kind vector?))
(s/def ::requires (s/coll-of (s/or :and-or #{:or :and}
                                   :expr ::requires*)
                             :kind vector?))

(s/def ::attributes (s/map-of core/attributes integer?))
(s/def ::skills (s/map-of core/skills true?))
(s/def ::unique true?)
(s/def ::effect
  (s/keys :opt-un [::attributes
                   ::skills]))

(s/def ::trait
  (s/keys :req-un [::name
                   ::description
                   ::requires]
          :opt-un [::unique
                   ::effect]))

(defn rule-matches-card? [rule {tags :tags :as card}]
  (cond
    (keyword? rule)
    (contains? (:tags card) rule)
    (= :or (first rule))
    (some #(rule-matches-card? % card) (rest rule))
    (= :and (first rule))
    (every? #(rule-matches-card? % card) (rest rule))
    (seq rule)
    (empty? (set/difference (set rule) tags))))

(defn rule-matches-deck? [rule cards]
  (cond
    (and (sequential? rule) (= :and (first rule)))
    (every? #(rule-matches-deck? % cards) (rest rule))
    (and (sequential? rule) (= :count 4 (first rule)))
    (>= (second rule)
        (count (filter #(rule-matches-card? (nth rule 2) %) cards)))
    :else
    (not-empty (filter #(rule-matches-card? rule %) cards))))

(defn count-matches [rule cards]
  (->> cards
       (map (partial rule-matches-card? rule))
       (filter some?)
       count))

#_(defn trait-matches? [{rule :requires :as trait} absences]
  (rule-matches? rule absences))