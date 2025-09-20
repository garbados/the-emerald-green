(ns the-emerald-green.help
  (:require
   #?(:clj
      [the-emerald-green.macros :refer [slurp-edn]]
      :cljs
      [the-emerald-green.macros :refer-macros [slurp-edn]])
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as g]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.deck :as deck]))

(def tag->tip
  (reduce
   merge
   (slurp-edn "help.edn")
   (for [id->thing [traits/id->trait
                    traits/id->talent
                    traits/id->ability
                    equipment/id->equipment
                    equipment/id->enchantment
                    deck/id->card]]
     (zipmap (keys id->thing) (map :description (vals id->thing))))))

(s/def ::tag
  (s/with-gen
    keyword?
    #(g/fmap identity (s/gen (set (keys tag->tip))))))

(s/def ::tag->tip
  (s/with-gen
    (s/map-of keyword? string?)
    #(g/fmap identity (g/return tag->tip))))

(s/def ::help-args
  (s/cat :thing (s/or :keyword ::tag
                      :string string?
                      :map map?)
         :tag->tip (s/? ::tag->tip)))

(defn get-help
  ([thing] (get-help thing tag->tip))
  ([thing tag->tip]
   (let [tip
         (cond
           (keyword? thing) (tag->tip thing)
           (and (string? thing)
                (seq thing)) thing
           (map? thing)
           (->> ((juxt :id :description :type) thing)
                (filter
                 #(cond
                    (string? %) (seq %)
                    :else (some? %)))
                (map #(get-help % tag->tip))
                (filter
                 #(cond
                    (string? %) (seq %)
                    :else (some? %)))
                first))]
     (cond
       tip tip
       (map? thing) (println "no tip:" (:id thing))
       (string? thing) nil
       :else (println "no tip:" (pr-str thing))))))

(s/fdef get-help
  :args ::help-args
  :ret (s/nilable string?))

(defn ^:no-stest ->title [tip]
  {:title tip
   :style "text-decoration: underline dotted; cursor: help;"})

(defn tag->title
  ([thing] (tag->title thing tag->tip))
  ([thing tag->tip]
   (when-let [tip (get-help thing tag->tip)]
     (->title tip))))

(s/def ::title string?)
(s/def ::style string?)
(s/def ::title-props
  (s/keys :req-un [::title
                   ::style]))
(s/fdef tag->title
  :args ::help-args
  :ret (s/nilable ::title-props))
