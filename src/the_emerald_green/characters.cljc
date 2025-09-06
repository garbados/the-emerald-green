(ns the-emerald-green.characters
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as g]
   [clojure.string :as string]
   [the-emerald-green.core :as core]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.money :as money]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.utils :as utils]))

(def max-level 20)

(s/def ::name string?)
(s/def ::biography string?)
(s/def ::level (s/int-in 1 (inc max-level)))
(s/def ::sanctified (s/coll-of ::deck/id :kind set?))
(s/def ::exiled (s/coll-of ::deck/id :kind set?))

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
                   ::equipment/equipped
                  ;;  ::equipment/inventory
                   ::media
                   ::money/wealth]))

(def base-character
  {:name ""
   :biography ""
   :level 1
   :sanctified #{}
   :exiled #{}
   :equipped []
   :inventory []
   :media []
   :wealth 1000})

(s/def ::talents (s/map-of ::traits/talent-id pos-int?))
(s/def ::abilities (s/coll-of ::traits/ability-id :kind set?))
(s/def ::traits (s/map-of ::traits/id pos-int?))
(s/def ::stats
  (s/keys :req-un [::core/attributes
                   ::core/skills
                   ::talents
                   ::abilities]))

(def base-attribute 2)

(def base-stats
  {:attributes (into {} (map #(vec [% base-attribute]) core/attributes))
   :skills (into {} (map #(vec [% false]) core/skills))
   :talents {}
   :abilities #{}})

(defn determine-stats [traits]
  (->> (for [[trait-id n] traits
             :let [{effect :effect} (traits/id->trait trait-id)]]
         (repeat n effect))
       (reduce concat [])
       (filter identity)
       (reduce
        (fn [stats {:keys [attributes skills talents abilities]}]
          (cond-> stats
            attributes (update :attributes (partial merge-with +) attributes)
            skills (update :skills merge skills)
            talents (update :talents
                            #(reduce
                              (fn [acc talent]
                                (update acc (:id talent talent) (fnil inc 0)))
                              % talents))
            abilities (update :abilities into (map #(:id % %) abilities))))
        base-stats)))

(s/fdef determine-stats
  :args (s/cat :traits (s/map-of ::traits/id (s/int-in 1 100)))
  :ret ::stats)

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
   (+ (:body attributes 0)
      (if (:resilience skills) level 0)
      level
      base-health)
   :draw
   (+ (:mind attributes 0)
      (if (:insight skills) 2 0)
      base-draw)
   :will
   (+ (:spirit attributes 0)
      (if (:resolve skills) 2 0)
      base-will)
   :fortune
   (+ (:luck attributes 0)
      (if (:theurgy skills) 2 0)
      base-fortune)
   :madness 0})

(s/fdef reset-fungibles
  :args (s/cat :character (s/keys :req-un [::core/attributes
                                           ::core/skills
                                           ::level]))
  :ret ::fungibles)

(defn merge-stats [{:keys [sanctified exiled level] :as character}]
  (let [absences (into sanctified exiled)
        traits (traits/determine-traits absences)
        stats (determine-stats traits)
        fungibles (reset-fungibles (merge stats {:level level}))]
    (merge {:fungibles fungibles}
           character ; let saved character override fungibles
           stats
           {:traits traits})))

(s/fdef merge-stats
  :args (s/cat :character (s/keys :req-un [::sanctified
                                           ::exiled
                                           ::level]))
  :ret (s/and ::stats (s/keys :req-un [::traits])))

;; "hydrated" character datastructure
(s/def ::character
  (s/with-gen
    (s/and
     ::persistent-character
     ::stats
     (s/keys :req-un [::fungibles
                      ::traits]))
    #(g/fmap
      (fn [[pchar traits stats fungibles]]
        (merge pchar
               {:traits traits}
               stats
               {:fungibles fungibles}))
      (s/gen (s/tuple ::persistent-character ::traits ::stats ::fungibles)))))

(def hydrated-character
  (merge base-character
         base-stats
         {:traits {}}
         {:fungibles base-fungibles}))

(defn merge-fungibles
  [character+stats & [fungibles]]
  (merge character+stats
         {:fungibles (or fungibles (reset-fungibles character+stats))}))

(s/fdef merge-fungibles
  :args (s/cat :character (s/with-gen
                            (s/and ::persistent-character
                                   ::stats)
                            #(g/fmap (partial apply merge) (s/gen (s/tuple ::persistent-character ::stats))))
               :fungibles (s/? ::fungibles))
  :ret (s/and ::persistent-character
              ::stats
              (s/keys :req-un [::fungibles])))

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
  (reduce dissoc character (keys base-stats)))

(s/fdef dehydrate-character
  :args (s/cat :character ::character)
  :ret ::persistent-character)

;; REPL BUDDIES

(defn ^:no-stest print-character [character]
  (println "#" (:name character) "<" (:level character) ">")
  (println "--")
  (doseq [prop [:biography :sanctified :exiled
                :equipped :wealth :inventory
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
    (doseq [attribute [:body :mind :spirit :luck]
            :let [value (get-in character [:attributes attribute])]
            :when value]
      (println (str (string/capitalize (name attribute)) ": " value)))
    (println "-- SKILLS")
    (doseq [[skill skilled?] (sort-by (comp name first) (:skills character))
            :when skilled?]
      (println (string/capitalize (name skill))))
    (when (-> character :talents seq)
      (println "-- TALENTS")
      (doseq [[talent-id n] (:talents character)
              :let [{talent-name :name :keys [description tags]} (traits/id->talent talent-id)]]
        (println (str talent-name
                      (if (< 1 n) (str " (x" n ")") "")
                      (if (seq tags) (str " {" (string/join ", " (map name tags)) "}") "")))
        (println "->" (string/replace description #"\n[\t ]+" "\n   "))))
    (println "-- ABILITIES")
    (doseq [ability-id (:abilities character)
            :let [{ability-name :name :keys [description phase actions madness tags]} (traits/id->ability ability-id)]]
      (println (str ability-name
                    " [" (name phase) ": " actions
                    (if (and madness (< 0 madness))
                      (str " (" madness "!)")
                      "")
                    "]"
                    (if (seq tags) (str " {" (string/join ", " (map name tags)) "}") "")))
      (println "->" (string/replace description #"\n[\t ]+" "\n   "))))
  (when (:fungibles character)
    (println "-- FUNGIBLES")
    (doseq [prop [:health :draw :will :fortune :madness]
            :let [value (get-in character [:fungibles prop])]
            :when value]
      (println (str (string/capitalize (name prop)) ": " value))))
  (println "-- TRAITS")
  (doseq [[trait-id n] (sort-by (comp name first) (:traits character))
          :let [{trait-name :name :keys [description]} (traits/id->trait trait-id)]]
    (println (str trait-name
                  (if (< 1 n) (str " (x" n ")") "")
                  ": " description))))

;; EXAMPLE CHARACTERS

(def dhutlo
  (hydrate-character
   (merge
    base-character
    {:name "Dhutlo K'smani"
     :biography "The *last* and *first* of Clan Quxot'l and G'xbenmi Fen."
     :sanctified #{:the-hermit :the-ace-of-swords}
     :exiled #{:the-page-of-swords :the-knight-of-cups :the-two-of-wands}})))

(def examples [dhutlo])
