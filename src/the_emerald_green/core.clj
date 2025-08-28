(ns the-emerald-green.core
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as g]
            [clojure.set :refer [difference]]))

(def suits #{:swords :wands :cups :pentacles})

(def minor-arcana-rank-names
  {2 "Two"
   3 "Three"
   4 "Four"
   5 "Five"
   6 "Six"
   7 "Seven"
   8 "Eight"
   9 "Nine"
   10 "Ten"
   11 "Page"
   12 "Knight"
   13 "Queen"
   14 "King"
   15 "Ace"})

(def minor-arcana
  (for [suit suits
        rank (drop 2 (range 16))]
    {:name (str "The " (minor-arcana-rank-names rank) " of " (string/capitalize (name suit)))
     :rank rank
     :tags
     [suit
      :minor-arcana
      (cond
        (< 10 rank 15) :court
        (zero? (mod rank 2)) :even
        :else :odd)]}))

(def major-arcana-lookup
  (reduce
   (fn [all arcana-name]
     (assoc all (keyword (string/lower-case (string/replace arcana-name " " "-"))) arcana-name))
   {}
   ["The Fool"
    "The Magician"
    "The High Priestess"
    "The Empress"
    "The Emperor"
    "The Hierophant"
    "The Lovers"
    "The Chariot"
    "Strength"
    "The Hermit"
    "Wheel of Fortune"
    "Justice"
    "The Hanged Man"
    "Death"
    "Temperance"
    "The Devil"
    "The Tower"
    "The Star"
    "The Moon"
    "The Sun"
    "Judgement"
    "The World"]))

(def major-arcana
  (for [[arcana-key arcana-name] major-arcana-lookup]
    {:name arcana-name
     :rank 16
     :tags [:major-arcana arcana-key]}))

(def base-deck (concat minor-arcana major-arcana))
(def gen-deck (constantly base-deck))

(s/def ::name (set (map :name base-deck)))
(s/def ::rank (s/int-in 2 17))
(s/def ::tags (s/coll-of
               (-> #{:odd :even :court :minor-arcana :major-arcana}
                   (into suits)
                   (into (set (keys major-arcana-lookup))))))

(s/def ::card
  (s/with-gen
    (s/keys :req-un [::name
                     ::rank
                     ::tags])
    #(s/gen base-deck)))

(s/def ::deck (s/coll-of ::card :distinct true :count 78))

(s/fdef gen-deck
  :args (s/cat)
  :ret ::deck)

(defn remove-card [deck card]
  (remove (partial = card) deck))

(s/fdef remove-card
  :args (s/cat :deck ::deck
               :card ::card)
  :ret ::deck)

(defn list-missing-cards [deck]
  (difference (set base-deck) (set deck)))

(s/fdef list-missing-cards
  :args (s/cat :deck ::deck)
  :ret ::deck)
