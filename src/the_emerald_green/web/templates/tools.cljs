(ns the-emerald-green.web.templates.tools
  (:require
   [clojure.string :as string]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.character :as c]))

(def base-stats
  {:attributes (into {} (map #(vec [% 0]) c/attributes))
   :skills (into {} (map #(vec [% false]) c/skills))
   :talents #{}
   :techniques #{}})

(defn determine-attributes [traits]
  (->> (map :effect traits)
       (filter identity)
       (reduce (fn [acc {:keys [attributes skills talents techniques]}]
                 (cond-> acc
                   attributes (update :attributes (partial merge-with +) attributes)
                   skills (update :skills merge skills)
                   talents (update :talents concat talents)
                   techniques (update :techniques concat techniques)))
               base-stats)))

(def base-fungibles
  {:fungibles (into {} (map #(vec [% 0]) c/fungibles))})

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

(def gameplay-extras
  {:draw-pile []
   :discard-pile []
   :hand []})

(defn list-cards [cards & {:keys [on-exile on-sanctify height]
                           :or {height "300px"}}]
  [:div.box
   {:style (str "overflow: scroll; height: " height ";")}
   [:table.table.is-hoverable
    [:thead
     [:tr
      [:th "Name"]
      [:th "Exile?"]
      [:th "Sanctify?"]]]
    (cons :tbody
          (for [card cards]
            [:tr
             [:td (:name card)]
             [:td [:button.button.is-small.is-danger
                   (if on-exile
                     {:onclick on-exile}
                     {:disabled true})
                   "Exile!"]]
             [:td [:button.button.is-small.is-info
                   (if on-sanctify
                     {:onclick on-sanctify}
                     {:disabled true})
                   "Sanctify!"]]]))]])

(defn new-character []
  [:div.content
   [:h1 "New Character"]
   [:div.columns
    [:div.column.is-narrow
     (list-cards deck/base-deck)]]
   [:div
    (for [card deck/base-deck]
      [:div.box
       [:h1 (:name card)]
       [:p "Rank: " (:rank card) " (+" (deck/rank->mod (:rank card)) ")"]
       [:p "Tags: " (string/join ", " (map deck/arcana-keyword->name (:tags card)))]])]])

(defn characters []
  [:div.content
   [:h1 "Characters"]
   [:p "TODO"]])

(defn campaigns []
  [:div.content
   [:h1 "Campaigns"]
   [:p "TODO"]])

(defn search [query]
  [:div.content
   [:h1 (str "Search: " query)]
   [:p "TODO"]])
