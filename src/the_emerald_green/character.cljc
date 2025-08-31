(ns the-emerald-green.character 
  (:require
   [clojure.spec.alpha :as s]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.traits :as traits]))

(def attribute->skills
  {:body   [:athletics :melee :ranged :resilience :intimidation]
   :mind   [:arcana :craft :diplomacy :insight :medicine]
   :spirit [:awareness :deception :sorcery :stealth :resolve]
   :luck   [:theurgy]})
(def attributes (set (keys attribute->skills)))
(def skills (reduce into #{} (vals attribute->skills)))
(def fungibles #{:health :will :luck :madness})

(s/def ::name string?)
(s/def ::biography string?)
(s/def ::level (s/int-in 1 20))
(s/def ::sanctified (s/coll-of ::deck/card :kind set?))
(s/def ::exiled (s/coll-of ::deck/card :kind set?))
(s/def ::wealth nat-int?)

(defn wealth-to-gold [wealth]
  (cond-> ""
    (> wealth 1000) (str (int (/ wealth 1000)) "p")
    (> (rem wealth 1000) 100) (str (int (/ (rem wealth 1000) 100)) "g")
    (> (rem wealth 100) 10) (str (int (/ (rem wealth 100) 10)) "s")
    (pos-int? (rem wealth 10)) (str (int (rem wealth 10)) "c")))

(s/fdef wealth-to-gold
  :args (s/cat :wealth ::wealth)
  :ret (partial re-matches #"(\d+p)?(\d+g)?(\d+s)?(\d+c)?"))

(s/def ::media-type keyword?) ; enumerated elsewhere TODO eventually
(s/def ::src string?)
(s/def ::media
  (s/coll-of
   (s/keys :req-un [::media-type
                    ::src]
           :opt-un [::description])
   :kind vector?))

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

(defn determine-traits [{:keys [sanctified exiled]}]
  (flatten
   (for [{rules :requires unique? :unique :as trait} traits/all-traits
         :let [absences (set (map deck/id->card (into sanctified exiled)))
               matches
               (if unique?
                 (traits/rule-matches-deck? rules absences)
                 (seq (filter (partial traits/rule-matches-card? rules) absences)))]
         :when matches]
     (if unique?
       trait
       (repeat (count matches) trait)))))

(s/fdef determine-traits
  :args (s/cat :character (s/keys :req-un [::sanctified ::exiled]))
  :ret (s/coll-of ::traits/trait))

(s/def ::attributes (s/map-of attributes pos-int?))
(s/def ::skills (s/map-of skills boolean?))
(s/def ::talent
  (s/keys :req-un [::name
                   ::description]))
(s/def ::talents (s/coll-of ::talent :kind set?))
(s/def ::ability
  (s/keys :req-un [::name
                   ::description
                   ::actions]))
(s/def ::abilities (s/coll-of ::ability :kind set?))

(s/def ::stats
  (s/keys :req-un [::attributes
                   ::skills
                   ::talents
                   ::abilities]))

(def base-stats
  {:attributes (into {} (map #(vec [% 0]) attributes))
   :skills (into {} (map #(vec [% false]) skills))
   :talents #{}
   :techniques #{}})

(defn determine-stats [traits]
  (->> (map :effect traits)
       (filter identity)
       (reduce (fn [acc {:keys [attributes skills talents techniques]}]
                 (cond-> acc
                   attributes (update :attributes (partial merge-with +) attributes)
                   skills (update :skills merge skills)
                   talents (update :talents concat talents)
                   techniques (update :techniques concat techniques)))
               base-stats)))

(s/fdef determine-stats
  :args (s/coll-of (s/keys :req-un [::traits/effect]))
  :ret ::stats)

(defn merge-stats [character]
  (merge character (determine-stats (determine-traits character))))

(s/def ::health nat-int?)
(s/def ::will nat-int?)
(s/def ::fortune nat-int?)
(s/def ::madness nat-int?)
(s/def ::fungibles
  (s/keys :req-un [::health
                   ::will
                   ::fortune
                   ::madness]))

(def base-fungibles (into {} (map #(vec [% 0]) fungibles)))

(def base-health 3)

(defn reset-fungibles [{:keys [attributes skills level]}]
  {:health (+ (:body attributes)
              (if (:resilience skills) level 0)
              level
              base-health)
   :will (+ (:mind attributes)
            (if (:insight skills) level 0))
   :luck (:luck attributes)
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
  (doseq [prop (keys (dissoc base-character :name :level))]
    (println prop (get character prop)))
  (when (seq (some (partial get character) (keys base-stats)))
    (println "-- STATS")
    (doseq [prop (keys base-stats)
            :when (get character prop)]
      (println prop (get character prop))))
  (when (seq (some (partial get character) (keys base-fungibles)))
    (println "-- FUNGIBLES")
    (doseq [prop (keys base-fungibles)
            :when (get character prop)]
      (println prop (get character prop)))))

;; EXAMPLE CHARACTERS

(def dhutlo
  (merge
   base-character
   {:name "Dhutlo K'smani"
    :biography "The *last* and *first* of Clan Quxot'l and G'xbenmi Fen."
    :sanctified #{:the-hermit :the-ace-of-swords}
    :exiled #{:the-two-of-wands :the-three-of-wands :the-four-of-wands}}))

(def examples [dhutlo])
