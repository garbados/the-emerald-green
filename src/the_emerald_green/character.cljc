(ns the-emerald-green.character
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [the-emerald-green.core :as core]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.money :as money]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.utils :as utils]))

(s/def ::name string?)
(s/def ::biography string?)
(s/def ::level (s/int-in 1 20))
(s/def ::sanctified (s/coll-of ::deck/card :kind set?))
(s/def ::exiled (s/coll-of ::deck/card :kind set?))

(s/def ::media-type keyword?) ; enumerated elsewhere TODO eventually
(s/def ::src string?)
(s/def ::description string?)
(s/def ::media
  (s/coll-of
   (s/keys :req-un [::media-type
                    ::src]
           :opt-un [::description])))

;; a version of a character that can be safely persisted to disk
(s/def ::persistent-character
  (s/keys :req-un [::name
                   ::biography
                   ::level
                   ::sanctified
                   ::exiled
                   ::equipment/equipment
                   ::equipment/inventory
                   ::media
                   ::wealth]))

(def base-character
  {:name ""
   :biography ""
   :level 1
   :sanctified #{}
   :exiled #{}
   :equipment []
   :inventory []
   :media []
   :wealth 1000})

(s/def ::stats
  (s/keys :req-un [::core/attributes
                   ::core/skills
                   ::core/talents
                   ::core/abilities]))

(def base-attribute 2)

(def base-stats
  {:attributes (into {} (map #(vec [% base-attribute]) core/attributes))
   :skills (into {} (map #(vec [% false]) core/skills))
   :talents {}
   :abilities #{}})

(defn determine-stats [traits]
  (->> (map (comp :effect traits/id->trait) traits)
       (filter identity)
       (reduce
        (fn [stats {:keys [attributes skills talents abilities]}]
          (cond-> stats
            attributes (update :attributes (partial merge-with +) attributes)
            skills (update :skills merge skills)
            talents (update :talents
                            #(reduce
                              (fn [acc talent] (update acc (:id talent talent) (fnil inc 0)))
                              % talents))
            abilities (update :abilities into (map #(:id % %) abilities))))
        base-stats)))

(s/fdef determine-stats
  :args (s/coll-of ::traits/id)
  :ret ::stats)

(defn merge-stats [{:keys [sanctified exiled] :as character}]
  (->> (into sanctified exiled)
       traits/determine-traits
       determine-stats
       (merge character)))

(s/def ::health nat-int?)
(s/def ::draw nat-int?)
(s/def ::will nat-int?)
(s/def ::fortune nat-int?)
(s/def ::madness nat-int?)
(s/def ::fungibles
  (s/keys :req-un [::health
                   ::draw
                   ::will
                   ::fortune
                   ::madness]))

(def base-fungibles (into {} (map #(vec [% 0]) core/fungibles)))

(def base-health 3)
(def base-draw 3)
(def base-will 2)
(def base-fortune 2)

(defn reset-fungibles [{:keys [attributes skills level]}]
  {:health
   (+ (:body attributes)
      (if (:resilience skills) level 0)
      level
      base-health)
   :draw
   (+ (:mind attributes)
      (if (:insight skills) 2 0)
      base-draw)
   :will
   (+ (:spirit attributes)
      (if (:resolve skills) 2 0)
      base-will)
   :fortune
   (+ (:luck attributes)
      (if (:theurgy skills) 2 0)
      base-fortune)
   :madness 0})

(s/fdef reset-fungibles
  :args (s/cat :character (s/keys :req-un [::attributes ::skills ::level]))
  :ret ::fungibles)

;; "hydrated" character datastructure
(s/def ::character
  (s/and
   ::persistent-character
   ::stats
   ::fungibles))

(def hydrated-character
  (merge base-character
         base-stats
         base-fungibles))

(defn merge-fungibles
  ([character+stats]
   (merge-fungibles character+stats (reset-fungibles character+stats)))
  ([character fungibles]
   (merge character fungibles)))

(s/fdef merge-fungibles
  :args (s/cat :character (s/and ::persistent-character
                                 ::stats)
               :fungibles (s/? ::fungibles))
  :ret ::character)

(defn ceil-fungibles [character]
  (merge-with max character base-fungibles))

(s/fdef ceil-fungibles
  :args (s/cat :character ::character)
  :ret ::character)

(defn hydrate-character [character]
  (-> character
      merge-stats
      merge-fungibles))

(s/fdef hydrate-character
  :args (s/cat :persistent-character ::persistent-character)
  :ret ::character)

(defn dehydrate-character [character]
  (reduce dissoc character (concat (keys base-stats) (keys base-fungibles))))

(s/fdef dehydrate-character
  :args (s/cat :character ::character)
  :ret ::persistent-character)

(defn print-character [character]
  (println "#" (:name character) "<" (:level character) ">")
  (println "--")
  (doseq [prop [:biography :sanctified :exiled
                :equipment :wealth :inventory
                :media]
          :let [prop-name (string/capitalize (name prop))
                prop-value* (get character prop)
                prop-value
                (cond
                  (#{:sanctified :exiled} prop)
                  (string/join ", " (map utils/keyword->name prop-value*))
                  (= :wealth prop)
                  (money/wealth-to-gold prop-value*)
                  :else
                  prop-value*)]
          :when (if (vector? prop-value)
                  (seq prop-value)
                  true)]
    (println prop-name ":" prop-value))
  (when (some (partial contains? character) (keys base-stats))
    (println "-- ATTRIBUTES")
    (doseq [attribute core/attributes
            :let [value (get-in character [:attributes attribute])]
            :when value]
      (println (string/capitalize (name attribute)) ":" value))
    (println "-- SKILLS")
    (doseq [[skill skilled?] (:skills character)
            :when skilled?]
      (println (string/capitalize (name skill))))
    (when (seq (filter (comp seq second) (select-keys character [:talents :abilities])))
      (println "-- STATS")
      (doseq [[prop value] (select-keys character [:talents :abilities])
              :when (seq value)]
        (println (string/capitalize (name prop)) ":" (string/join "," (map :name value))))))
  (when (some (partial contains? character) core/fungibles)
    (println "-- FUNGIBLES")
    (doseq [prop (keys base-fungibles)
            :when (get character prop)]
      (println (string/capitalize (name prop)) ":" (get character prop)))))

;; EXAMPLE CHARACTERS

(def dhutlo
  (hydrate-character
   (merge
    base-character
    {:name "Dhutlo K'smani"
     :biography "The *last* and *first* of Clan Quxot'l and G'xbenmi Fen."
     :sanctified #{:the-hermit :the-ace-of-swords}
     :exiled #{:the-two-of-wands :the-three-of-wands :the-four-of-wands}})))

(def examples [dhutlo])
