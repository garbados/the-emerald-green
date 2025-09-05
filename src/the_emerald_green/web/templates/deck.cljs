(ns the-emerald-green.web.templates.deck
  (:require
   [clojure.string :as string]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.utils :refer [refresh-node]]))

;; FIXME finish this obsessive little quest
(def the-order-of-things
  [:the-fool
   :the-two-of-swords
   :the-two-of-wands
   :the-two-of-cups
   :the-two-of-pentacles])

(defn describe-card [{card-id :id
                      card-name :name
                      :keys [tags]}]
  [:div.box
   [:h3 card-name]
   [:p (-> card-id deck/card-metadata :description)]
   [:p "Tags:"]
   [:pre>code (with-out-str (println tags))]
   (when-let [trait-reqs
              (->> (map #(select-keys % [:card :deck]) traits/traits)
                   vals
                   flatten
                   (filter keyword?)
                   set
                   seq)]
     [:div
      [:p "Found in these traits:"]
      [:ul
       (for [{trait-name :name} traits/traits
             :when (contains? trait-reqs card-id)]
         [:li [:p trait-name]])]])])

(defn card-matches?
  [{card-name :name
    :keys [description tags]}
   re]
  (or (re-find re card-name)
      (re-find re description)
      (re-find re (string/join ", " tags))))

(defn list-cards [query]
  (for [card deck/base-deck
        :let [re (re-pattern query)]
        :when (card-matches? card re)]
    (describe-card card)))

(defn card-guide []
  (let [-query (atom "")
        refresh-cards #(refresh-node "cards" (partial list-cards @-query))]
    (add-watch -query :query refresh-cards)
    [:div.content
     [:h1 "Card Guide"]
     [:p "A 78-card Tarot deck has the following cards. They tell a kind of story, and contain many fates."]
     (prompts/text -query
                   :placeholder "🔍 Filter cards by name, description, or requirements.")
     [:hr]
     [:div#cards (list-cards @-query)]]))
