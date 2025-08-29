(ns the-emerald-green.deck
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as s]
            [clojure.set :as set]))

(def suits #{:swords :wands :cups :pentacles})

(defn arcana-name->keyword [arcana-name]
  (keyword (string/lower-case (string/replace arcana-name " " "-"))))

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
        rank (drop 2 (range 16))
        :let [arcana-name (str "The " (minor-arcana-rank-names rank) " of " (string/capitalize (name suit)))
              arcana-kw (arcana-name->keyword arcana-name)]]
    {:name arcana-name
     :id arcana-kw
     :rank rank
     :tags
     #{suit
       arcana-kw
       :minor-arcana
       (cond
         (< 10 rank 15) :court
         (zero? (mod rank 2)) :even
         :else :odd)}}))

(def major-arcana
  (for [arcana-name
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
         "The World"]
         :let [arcana-kw (arcana-name->keyword arcana-name)]]
    {:name arcana-name
     :id arcana-kw
     :rank 16
     :tags #{:major-arcana arcana-kw}}))

(def base-deck (concat minor-arcana major-arcana))
(def gen-deck (constantly base-deck))

(s/def ::name (set (map :name base-deck)))
(s/def ::id (set (map :id base-deck)))
(s/def ::rank (s/int-in 2 17))
(s/def ::tag (reduce into #{} (map :tags base-deck)))
(s/def ::tags (s/coll-of ::tag :kind set?))

(s/def ::card*
  (s/keys :req-un [::name
                   ::id
                   ::rank
                   ::tags]))

(s/def ::card (set base-deck))
(s/def ::deck (s/coll-of ::card :distinct true :max-count 78))

(s/fdef gen-deck
  :args (s/cat)
  :ret ::deck)

(s/fdef arcana-name->keyword
  :args (s/cat :name (set (map :name base-deck)))
  :ret ::tag)

(defn remove-card [deck card]
  (remove (partial = card) deck))

(s/fdef remove-card
  :args (s/cat :deck ::deck
               :card ::card)
  :ret ::deck)

(defn remove-cards-by-tag [deck tag]
  (remove #(contains? (:tags %) tag) deck))

(s/fdef remove-cards-by-tag
  :args (s/cat :deck ::deck
               :card ::card)
  :ret ::deck)

(defn list-missing-cards [deck]
  (set/difference (set base-deck) (set deck)))

(s/fdef list-missing-cards
  :args (s/cat :deck ::deck)
  :ret ::deck)
