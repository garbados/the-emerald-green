(ns the-emerald-green.web.templates.traits
  (:require
   ["marked" :as marked]
   [clojure.string :as string]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.utils :refer [keyword->name]]
   [the-emerald-green.web.alchemy :refer [profane]]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.utils :refer [lolraw refresh-node]]
   [the-emerald-green.core :as core]
   [the-emerald-green.help :as help]
   [the-emerald-green.deck :as deck]))

(defn describe-reqs [reqs]
  (cond
    (keyword? reqs)
    [:span
     (help/tag->title
      (or (traits/id->trait reqs)
          (deck/id->card reqs)
          reqs))
     (keyword->name reqs)]
    (= 1 (count reqs)) (describe-reqs (first reqs))
    ((into traits/bool-reqs traits/not-req) (first reqs))
    (let [[kw & subreqs] reqs]
      [[:span (help/tag->title kw) (traits/req->help kw)]
       [:ul
        (map (comp #(vec [:li %]) describe-reqs) subreqs)]])
    (traits/comp-reqs (first reqs))
    (let [[kw n & subreqs] reqs]
      [[:span (traits/req->help kw) " " n]
       [:ul
        (map (comp #(vec [:li %]) describe-reqs) subreqs)]])
    :else [:ul (map (comp #(vec [:li %]) describe-reqs) reqs)]))

(defn describe-talent [talent-ish]
  (let [{talent-name :name
         :keys [description]}
        (if (keyword? talent-ish) (traits/id->talent talent-ish) talent-ish)]
    [:div.block
     [:p [:em talent-name] ": " description]]))

(defn describe-ability [ability-ish]
  (let [{ability-name :name
         :keys [description actions phase tags madness]}
        (if (keyword? ability-ish) (traits/id->ability ability-ish) ability-ish)]
    [:div.block
     [:p [:strong>em ability-name]]
     [:blockquote
      (profane "p" (marked/parse description))]
     [:ul
      [:li [:span (help/tag->title :phase) "Phase"] ": " [:span (help/tag->title phase) (keyword->name phase)]]
      [:li [:span (help/tag->title :actions) "Actions"] ": " actions]
      (when madness
        [:li [:span (help/tag->title :ability-madness) "Madness"] ": " madness])
      (when tags
        [:li "Tags: "
         (interpose
          ", "
          (for [tag tags
                :let [tag-name (keyword->name tag)]]
            [:span (help/tag->title tag) tag-name]))])]]))

(defn describe-trait
  ([{trait-name :name
     :keys [effect description]
     :as trait}
    & [n]]
   [:div.block>div.box>div.content
    [:p.subtitle trait-name (when (< 1 n) (str " (x " n ")"))]
    [:blockquote
     (profane "p" (marked/parse description))]
    (when effect
      [[:p "Effects"]
       (let [{:keys [attributes skills fungibles talents abilities]} effect]
         [:ul
          (when (seq attributes)
            [:li
             [:p "Attributes: "
              (interpose
               ", "
               (for [attr core/attr-order
                     :let [value (get attributes attr)
                           attr-span [:span (help/tag->title attr) (keyword->name attr)]]
                     :when value]
                 [:span attr-span ": " (if (pos? value) (str "+" value) value)]))]])
          (when (seq skills)
            [:li
             [:p "Skills: "
              (interpose
               ", "
               (for [skill core/ordered-skills
                     :let [value (get skills skill)]
                     :when value]
                 [:span (help/tag->title skill) (keyword->name skill)]))]])
          (when (seq fungibles)
            [:li
             [:p "Fungibles: "
              (interpose
               ", "
               (for [fung core/fung-order
                     :let [value (get fungibles fung)
                           attr-span [:span (help/tag->title fung) (keyword->name fung)]]
                     :when value]
                 [:span attr-span ": " (if (pos? value) (str "+" value) value)]))]])
          (when (seq talents)
            [:li
             [:p "Talents"]
             [:ul (map (comp #(vec [:li %]) describe-talent) talents)]])
          (when (seq abilities)
            [:li
             [:p "Abilities"]
             [:ul (map (comp #(vec [:li %]) describe-ability) abilities)]])])])
    [:details
     [:summary [:em "Requirements"]]
     [:ul
      (for [[k title] [[:traits "Traits"]
                       [:deck "Deck"]
                       [:card "Card"]]
            :let [reqs (get trait k)]
            :when reqs]
        [:li
         [:p (help/tag->title k) title]
         [:ul [:li (describe-reqs reqs)]]])]]
    [:details
     [:summary [:em "Definition"]]
     (lolraw trait)]]))

(def describe-a-trait (memoize #(describe-trait %)))

(defn join-reqs [reqs]
  (if (keyword? reqs)
    (name reqs)
    (->> reqs
         flatten
         set
         (filter keyword?)
         (map name)
         (map #(string/split % #"-"))
         flatten
         set
         (string/join " "))))

(defn trait-matches? [trait re]
  (let [{card-reqs :card
         deck-reqs :deck
         trait-reqs :traits
         trait-name :name
         :keys [description]} trait]
    (or (when card-reqs (re-find re (join-reqs card-reqs)))
        (when deck-reqs (re-find re (join-reqs deck-reqs)))
        (when trait-reqs (re-find re (join-reqs trait-reqs)))
        (re-find re (string/lower-case trait-name))
        (re-find re (string/lower-case description)))))

(defn list-traits
  ([] (list-traits ""))
  ([query] (list-traits query traits/traits))
  ([query traits]
   (let [sorted (sort-by :name traits)
         re (re-pattern query)]
     [:div
      (if (seq query)
        (->> sorted
             (filter #(trait-matches? % re))
             (sort-by :name)
             (map describe-a-trait))
        (map describe-a-trait sorted))])))

(defn traits-guide []
  (let [-query (atom "")
        refresh-traits #(refresh-node "traits" (partial list-traits @-query))]
    (add-watch -query :query refresh-traits)
    [:div.content
     [:h1 "Trait Guide"]
     [:div.block
      [:p "Here are documented all the fae traits you may... develop."]
      (prompts/text -query
                    :placeholder "🔍 Filter cards by name, description, or requirements.")]
     [:div.block#traits (list-traits @-query)]]))
