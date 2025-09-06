(ns the-emerald-green.challenges
  (:require
   [clojure.spec.alpha :as s]
   [the-emerald-green.deck :as deck]))

(s/def ::difficulty (s/and pos-int? #(< 3 %)))
(s/def ::demand (s/nilable ::deck/tag))
(s/def ::challenge (s/tuple ::difficulty ::demand))

(defn propose-challenge [difficulty demand]
  [difficulty demand])

(s/fdef propose-challenge
  :args (s/cat :difficulty ::difficulty
               :demand ::demand)
  :ret ::challenge)

(defn answer-challenge
  ([challenge card] (answer-challenge challenge card 0))
  ([[difficulty demand]
    {:keys [tags rank]}
    modifier]
   (let [effective-rank
         (cond-> (deck/rank->mod rank)
           (and demand
                (false? (contains? tags demand)))
           (- 2)
           (number? modifier)
           (+ modifier))]
     (<= difficulty effective-rank))))

(s/fdef answer-challenge
  :args (s/cat :challenge ::challenge
               :response ::deck/card
               :modifier (s/? nat-int?))
  :ret boolean?)

;; with modifier=0
;; organize into:
;; difficulty => % of base deck that meets or beats
#_(doseq [demand (concat deck/suits [:even :odd :minor-arcana :major-arcana])]
  (println (name demand))
  (doseq [i (range 2 8)
          :let [challenge (propose-challenge i demand)
                answers-challenge? (partial answer-challenge challenge)
                n
                (->> deck/base-deck
                     (filter answers-challenge?)
                     (map :name)
                     count
                     (#(/ % (count deck/base-deck)))
                     (* 100)
                     int)]]
    (println i ":" n "%")))
