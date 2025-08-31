(ns the-emerald-green.character 
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
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
(def fungibles #{:health :draw :will :fortune :madness})

(s/def ::name string?)
(s/def ::biography string?)
(s/def ::level (s/int-in 1 20))
(s/def ::sanctified (s/coll-of ::deck/card :kind set?))
(s/def ::exiled (s/coll-of ::deck/card :kind set?))
(s/def ::wealth nat-int?)

(def x-copper 1)
(def x-silver 100)
(def x-gold (* x-silver 100))
(def x-platinum (* x-gold 100))

(def gold-expr #"^(\d+p)?(\d+g)?(\d+s)?(\d+c)?$")

(defn wealth-to-gold [wealth]
  (cond-> ""
    (>= wealth x-platinum) (str (int (/ wealth x-platinum)) "p")
    (> (rem wealth x-platinum) x-gold) (str (int (/ (rem wealth x-platinum) x-gold)) "g")
    (> (rem wealth x-gold) x-silver) (str (int (/ (rem wealth x-gold) x-silver)) "s")
    (pos-int? (rem wealth x-silver)) (str (int (rem wealth x-silver)) "c")))

(s/fdef wealth-to-gold
  :args (s/cat :wealth ::wealth)
  :ret (partial re-matches gold-expr))

(defn gold-to-wealth [gold-ish]
  (when-let [match (re-matches gold-expr gold-ish)]
    (->> (for [[coin weight] (map vector (rest match) [x-platinum x-gold x-silver x-copper])
               :when coin
               :let [wealth ((comp
                              #?(:cljs js/parseInt
                                 :clj Integer/parseInt)
                              second
                              (partial re-matches #"^(\d+)[pgsc]$"))
                             coin)]]
           (* wealth weight))
         (reduce + 0))))

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

(defn determine-traits
  ([{:keys [sanctified exiled]}] (determine-traits sanctified exiled))
  ([sanctified exiled]
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
        (repeat (count matches) trait))))))

(s/fdef determine-traits
  :args (s/or
         :explicit
         (s/cat :sanctified ::sanctified
                :exiled ::exiled)
         :character
         (s/cat :character (s/keys :req-un [::sanctified ::exiled])))
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

(def base-attribute 2)

(def base-stats
  {:attributes (into {} (map #(vec [% base-attribute]) attributes))
   :skills (into {} (map #(vec [% false]) skills))
   :talents #{}
   :abilities #{}})

(defn determine-stats [traits]
  (->> (map :effect traits)
       (filter identity)
       (reduce (fn [acc {:keys [attributes skills talents abilities]}]
                 (cond-> acc
                   attributes (update :attributes (partial merge-with +) attributes)
                   skills (update :skills merge skills)
                   talents (update :talents concat talents)
                   abilities (update :abilities concat abilities)))
               base-stats)))

(s/fdef determine-stats
  :args (s/coll-of (s/keys :req-un [::traits/effect]))
  :ret ::stats)

(defn merge-stats [character]
  (merge character (determine-stats (determine-traits character))))

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

(def base-fungibles (into {} (map #(vec [% 0]) fungibles)))

(def base-health 3)

(defn reset-fungibles [{:keys [attributes skills level]}]
  {:health (+ (:body attributes)
              (if (:resilience skills) level 0)
              level
              base-health)
   :hand (+ (:mind attributes)
            (if (:insight skills) level 0))
   :will (+ (:spirit attributes)
            (if (:resolve skills) level 0))
   :fortune (:luck attributes)
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
                  (string/join ", " (map deck/arcana-keyword->name prop-value*))
                  (= :wealth prop)
                  (wealth-to-gold prop-value*)
                  :else
                  prop-value*)]
          :when (if (vector? prop-value)
                  (seq prop-value)
                  true)]
    (println prop-name ":" prop-value))
  (when (some (partial contains? character) (keys base-stats))
    (println "-- ATTRIBUTES")
    (doseq [attribute attributes
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
  (when (some (partial contains? character) fungibles)
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
