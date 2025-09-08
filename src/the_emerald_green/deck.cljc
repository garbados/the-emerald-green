(ns the-emerald-green.deck
  (:require
   #?(:clj
      [the-emerald-green.macros :refer [slurp-edn]]
      :cljs
      [the-emerald-green.macros :refer-macros [slurp-edn]])
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [the-emerald-green.utils :refer [name->keyword]]))

;; an obsessive little quest
(def the-order-of-things
  [:the-fool
   :the-two-of-swords
   :the-two-of-wands
   :the-two-of-cups
   :the-two-of-pentacles
   :the-magician
   :the-high-priestess
   :the-three-of-swords
   :the-three-of-wands
   :the-three-of-cups
   :the-three-of-pentacles
   :the-empress
   :the-emperor
   :the-four-of-swords
   :the-four-of-wands
   :the-four-of-cups
   :the-four-of-pentacles
   :the-hierophant
   :the-five-of-swords
   :the-five-of-wands
   :the-five-of-cups
   :the-five-of-pentacles
   :the-lovers
   :the-six-of-swords
   :the-six-of-wands
   :the-six-of-cups
   :the-six-of-pentacles
   :the-chariot
   :strength
   :the-seven-of-swords
   :the-seven-of-wands
   :the-seven-of-cups
   :the-seven-of-pentacles
   :the-hermit
   :the-eight-of-wands
   :the-eight-of-cups
   :the-eight-of-swords
   :the-eight-of-pentacles
   :the-hanged-man
   :death
   :the-nine-of-wands
   :the-nine-of-cups
   :the-nine-of-swords
   :the-nine-of-pentacles
   :temperance
   :the-ten-of-swords
   :the-ten-of-wands
   :the-ten-of-cups
   :the-ten-of-pentacles
   :the-devil
   :the-tower
   :the-page-of-swords
   :the-page-of-wands
   :the-page-of-cups
   :the-page-of-pentacles
   :wheel-of-fortune
   :justice
   :the-knight-of-swords
   :the-knight-of-wands
   :the-knight-of-cups
   :the-knight-of-pentacles
   :the-star
   :the-queen-of-swords
   :the-queen-of-wands
   :the-queen-of-cups
   :the-queen-of-pentacles
   :the-moon
   :the-king-of-swords
   :the-king-of-wands
   :the-king-of-cups
   :the-king-of-pentacles
   :the-sun
   :judgement
   :the-ace-of-swords
   :the-ace-of-wands
   :the-ace-of-cups
   :the-ace-of-pentacles
   :the-world])

(def id->metadata (slurp-edn "cards.edn"))

(def ^:no-stest clean-description #(string/replace % #"\n\s+" "\n"))

(def suits [:swords :wands :cups :pentacles])

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
              arcana-kw (name->keyword arcana-name)
              {:keys [description]} (id->metadata arcana-kw)]]
    {:name arcana-name
     :description (clean-description description)
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
        :let [arcana-kw (name->keyword arcana-name)
              {:keys [description]} (id->metadata arcana-kw)]]
    {:name arcana-name
     :description (clean-description description)
     :id arcana-kw
     :rank 16
     :tags #{:major-arcana arcana-kw}}))

(def base-deck (concat minor-arcana major-arcana))
(def base-deck-set (set base-deck))
(def id->card (into {} (map (juxt :id identity) base-deck)))
(def the-ordered-deck (vec (map id->card the-order-of-things)))

(s/def ::name (set (map :name base-deck)))
(s/def ::id (set (map :id base-deck)))
(s/def ::rank (s/int-in 2 17))
(s/def ::effective-rank (s/int-in 2 8))
(s/def ::tag (reduce into #{} (map :tags base-deck)))
(s/def ::tags (s/coll-of ::tag :kind set?))

(s/def ::card*
  (s/keys :req-un [::name
                   ::id
                   ::rank
                   ::tags]))

(s/def ::card base-deck-set)
(s/def ::card-ids (s/coll-of ::id :distinct true))
(s/def ::cards (s/coll-of ::card :distinct true))

(defn rank->mod [rank]
  (cond
    (<= 2 rank 4) 2
    (<= 5 rank 7) 3
    (<= 8 rank 10) 4
    (<= 11 rank 13) 5
    (<= 14 rank 15) 6
    :else 7))

(s/fdef rank->mod
  :args (s/cat :rank ::rank)
  :ret ::effective-rank)

(defn card-matches-re?
  [{card-name :name
    :keys [description tags]}
   re]
  (some?
   (or (re-find re card-name)
       (re-find re description)
       (re-find re (string/join ", " tags)))))

(s/fdef card-matches-re?
  :args (s/cat :card ::card
               :re :re/pattern)
  :ret boolean?)
