(ns the-emerald-green.traits
  (:require
   #?(:clj
      [the-emerald-green.macros :refer [slurp-dir-edn]]
      :cljs
      [the-emerald-green.macros :refer-macros [slurp-dir-edn]])
   [clojure.spec.alpha :as s]
   [the-emerald-green.core :as core]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.utils :as utils]))

(def traits
  (->> (slurp-dir-edn "traits")
       (map utils/idify)
       (map
        (fn [trait]
          (if-let [talents (get-in trait [:effect :talents])]
            (assoc-in trait [:effect :talents] (map utils/idify talents))
            trait)))
       (map
        (fn [trait]
          (if-let [abilities (get-in trait [:effect :abilities])]
            (assoc-in trait [:effect :abilities] (map utils/idify abilities))
            trait)))))
(def id->trait (reduce utils/merge-by-id {} traits))

(def talents (utils/uniq-defs (map #(get-in % [:effect :talents]) traits)))
(def id->talent (reduce utils/merge-by-id {} talents))

(def abilities (utils/uniq-defs (map #(get-in % [:effect :abilities]) traits)))
(def id->ability (reduce utils/merge-by-id {} abilities))

(s/def ::name string?)
(s/def ::description string?)
(s/def ::id (set (keys id->trait)))

(def not-req #{:not})
(def bool-reqs #{:and :or})
(def comp-reqs #{:gte :gt :lt :lte})

(defn ^:no-stest req->bool-fn [bool-kw match-fn]
  (cond
    (= :and bool-kw)
    (fn [rule coll]
      (every? #(match-fn % coll) rule))
    (= :or bool-kw)
    (fn [rule coll]
      (true? (some #(match-fn % coll) rule)))))

(def req->comp
  {:gte <=
   :gt <
   :lte >=
   :lt >})

(def req->help
  {:not "Not"
   :or "Or"
   :and "And"
   :gte "Needs at least"
   :gt "Needs more than"
   :lte "Needs at most"
   :lt "Needs less than"})

(defn ^:no-stest sdef-match-syntax [reqspec idspec]
  (s/def reqspec
    (s/or :not-req
          (s/cat :pred not-req
                 :expr (s/+ reqspec))
          :bool-req
          (s/cat :pred bool-reqs
                 :expr (s/+ reqspec))
          :comp-req
          (s/cat :pred comp-reqs
                 :n pos-int?
                 :expr (s/+ reqspec))
          :expr (s/coll-of idspec :kind vector?)
          :solo idspec)))

(sdef-match-syntax :match*/deck ::deck/tag)
(sdef-match-syntax :match*/card ::deck/tag)
(sdef-match-syntax :match*/traits ::id)

(s/def :match/deck (->> traits (map :deck) (filter some?) set))
(s/def :match/card (->> traits (map :card) (filter some?) set))
(s/def :match/traits (->> traits (map :traits) (filter some?) set))

(s/def ::talent-id (set (keys id->talent)))
(s/def ::talent
  (s/or :def (set talents)
        :ref ::talent-id))
(s/def ::talents (s/coll-of ::talent))
(s/def ::ability-id (set (keys id->ability)))
(s/def ::ability
  (s/or :def (set abilities)
        :ref ::ability-id))
(s/def ::abilities (s/coll-of ::ability))
(s/def ::effect
  (s/keys :opt-un [::core/attributes
                   ::core/skills
                   ::core/fungibles
                   ::talents
                   ::abilities]))
(s/def ::trait*
  (s/keys :req-un [::name
                   ::description]
          :opt-un [::effect
                   :match*/traits
                   :match*/deck
                   :match*/card]))
(s/def ::trait (set traits))

(def rule-matches-card?
  (memoize
   (fn [rule {tags :tags :as card}]
     (cond
       (keyword? rule)
       (contains? tags rule)
       (contains? not-req (first rule))
       (not (rule-matches-card? (rest rule) card))
       (contains? bool-reqs (first rule))
       (let [[bool-kw & subrule] rule
             bool-fn (req->bool-fn bool-kw rule-matches-card?)]
         (bool-fn subrule card))
       (sequential? rule)
       (every? #(rule-matches-card? % card) rule)))))

(s/fdef rule-matches-card?
  :args (s/cat :rule :match/card
               :card ::deck/card)
  :ret boolean?)

(defn rule-matches-cards? [rule cards]
  (cond
    (keyword? rule)
    (true? (some (partial rule-matches-card? rule) cards))
    (contains? not-req (first rule))
    (not (rule-matches-cards? (rest rule) cards))
    (bool-reqs (first rule))
    (let [[bool-kw & subrule] rule
          bool-fn (req->bool-fn bool-kw rule-matches-cards?)]
      (bool-fn subrule cards))
    (comp-reqs (first rule))
    (let [[comp-req boundary & rest-rule] rule]
      ((req->comp comp-req)
       boundary
       (count (filter #(rule-matches-card? rest-rule %) cards))))
    (sequential? rule)
    (every? #(rule-matches-cards? % cards) rule)))

(s/fdef rule-matches-cards?
  :args (s/cat :rule :match/deck
               :card ::deck/cards)
  :ret boolean?)

(defn rule-matches-traits? [rule traits]
  (cond
    (keyword? rule)
    (if (keyword? traits)
      (= rule traits)
      (contains? (set traits) rule))
    (contains? not-req (first rule))
    (not (rule-matches-traits? (rest rule) traits))
    (bool-reqs (first rule))
    (let [[bool-kw & subrule] rule
          bool-fn (req->bool-fn bool-kw rule-matches-traits?)]
      (bool-fn subrule traits))
    (comp-reqs (first rule))
    (let [[comp-req boundary & rest-rule] rule]
      ((req->comp comp-req)
       boundary
       (count (filter #(rule-matches-traits? rest-rule %) traits))))
    (sequential? rule)
    (every? #(rule-matches-traits? % traits) rule)))

(s/fdef rule-matches-traits?
  :args (s/cat :rule :match/traits
               :traits (s/coll-of ::id))
  :ret (s/nilable boolean?))

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
               [id (count (filter (partial rule-matches-card? rule) cards))])
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
     (for [id (-> deck-matches
                  (into (keys card-matches))
                  (into trait-matches))
           :let [trait (id->trait id)]
           :when (and
                  (if (:deck trait) (contains? deck-matches id) true)
                  (if (:card trait) (contains? card-matches id) true)
                  (if (:traits trait) (contains? trait-matches id) true))]
       [id (if (:card trait) (get card-matches id) 1)])
     (into {}))))

(s/fdef determine-traits
  :args (s/cat :card-ids ::deck/card-ids)
  :ret (s/map-of ::id pos-int?))
