(ns the-emerald-green.traits
  (:require
   [clojure.set :as set]
   [clojure.spec.alpha :as s]
   [the-emerald-green.core :as core]
   [the-emerald-green.deck :as deck]
   #?(:clj
      [the-emerald-green.utils :refer [slurp-dir-edn sdef-match-syntax]]
      :cljs
      [the-emerald-green.utils :refer-macros [slurp-dir-edn sdef-match-syntax]])))

(def traits
  (map #(assoc % :id (deck/arcana-name->keyword (:name %)))
       (slurp-dir-edn "resources/traits")))

(def id->trait
  (reduce
   (fn [acc {id :id :as trait}] (assoc acc id trait))
   {}
   traits))

(def trait-ids (set (keys id->trait)))

(s/def ::name string?)
(s/def ::description string?)
(s/def ::id trait-ids)
(sdef-match-syntax :match/deck* ::deck/tag)
(sdef-match-syntax :match/card* ::deck/tag)
(sdef-match-syntax :match/traits* ::id)
(s/def :match/traits (->> traits (map :traits) (filter some?) set))
(s/def :match/deck (->> traits (map :deck) (filter some?) set))
(s/def :match/card (->> traits (map :card) (filter some?) set))
(s/def ::effect
  (s/keys :opt-un [::core/attributes
                   ::core/skills
                   ::core/talents
                   ::core/abilities]))
(s/def ::trait*
  (s/keys :req-un [::name
                   ::description]
          :opt-un [::effect
                   :match/traits
                   :match/deck
                   :match/card]))
(s/def ::trait (set traits))

(def rule-matches-card?
  (memoize
   (fn [rule {tags :tags :as card}]
     (cond
       (keyword? rule)
       (contains? tags rule)
       (= :or (first rule))
       (true? (some #(rule-matches-card? % card) (rest rule)))
       (set? rule)
       (empty? (set/difference rule tags))
       (= :and (first rule))
       (every? #(rule-matches-card? % card) (rest rule))
       (sequential? rule)
       (every? #(rule-matches-card? % card) rule)))))

(s/fdef rule-matches-card?
  :args (s/cat :rule ::requires
               :card ::deck/card)
  :ret boolean?)

(defn rule-matches-cards? [rule cards]
  (cond
    (and (sequential? rule) (= :and (first rule)))
    (every? #(rule-matches-cards? % cards) (rest rule))
    (and (sequential? rule) (= :count (first rule)))
    (>= (second rule)
        (count (filter #(rule-matches-card? (nth rule 2) %) cards)))
    :else
    (-> (filter (partial rule-matches-card? rule) cards)
        count
        pos-int?)))

(s/fdef rule-matches-cards?
  :args (s/cat :rule ::requires
               :card ::deck/deck)
  :ret boolean?)

(defn rule-matches-traits? [rule traits]
  (cond
    (keyword? rule) (contains? (set traits) rule)
    (= :and (first rule)) (every? #(rule-matches-traits? % traits) (rest rule))
    (= :or (first rule)) (some #(rule-matches-traits? % traits) (rest rule))
    (= :count (first rule))
    (<= (second rule)
        (count (filter #(rule-matches-traits? (drop 2 rule) [%]) traits)))
    (sequential? rule) (some? (seq (set/difference (set rule) (set traits))))))

(defn determine-traits [card-ids]
  (let [cards (map deck/id->card card-ids)
        deck-matches
        (set
         (for [{rule :deck id :id} traits
               :when (and rule (rule-matches-cards? rule cards))]
           id))
        card-matches
        (->> (for [{rule :card id :id} traits
                   :when rule]
               [id (count (map (partial rule-matches-card? rule) cards))])
             (filter (comp pos-int? second))
             (into {}))
        traits-so-far
        (->> (for [[id n] card-matches]
               (repeat n id))
             (reduce concat deck-matches))
        trait-matches
        (set
         (for [{rule :traits id :id} traits
               :when (and rule (rule-matches-traits? rule traits-so-far))]
           id))]
    (->>
     (for [{id :id :as trait} traits
           :when (and
                  (if (:cards trait) (contains? deck-matches id) true)
                  (if (:card trait) (contains? card-matches id) true)
                  (if (:traits trait) (contains? trait-matches id) true))]
       [id (if (:card trait) (get card-matches id) 1)])
     (into {}))))
