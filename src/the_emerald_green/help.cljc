(ns the-emerald-green.help
  (:require
   #?(:clj
      [the-emerald-green.macros :refer [slurp-edn]]
      :cljs
      [the-emerald-green.macros :refer-macros [slurp-edn]])
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as g]))

(def tag->tip (slurp-edn "help.edn"))

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
           (->> ((juxt :id :description :biography :type) thing)
                (filter some?)
                (map #(get-help % tag->tip))
                (filter some?)
                first))]
     (if tip tip (println "no tip:" thing)))))

(s/fdef get-help
  :args ::help-args
  :ret (s/nilable string?))

(defn tag->title
  ([thing] (tag->title thing tag->tip))
  ([thing tag->tip]
   (when-let [tip (get-help thing tag->tip)]
     {:title tip
      :style "text-decoration: underline dotted; cursor: help;"})))

(s/def ::title string?)
(s/def ::style string?)
(s/def ::title-props
  (s/keys :req-un [::title
                   ::style]))
(s/fdef tag->title
  :args ::help-args
  :ret (s/nilable ::title-props))

(def markdown-tip
  [:em "Use " [:a {:href "https://commonmark.org/help/" :target "_blank"} "Markdown!"]])
