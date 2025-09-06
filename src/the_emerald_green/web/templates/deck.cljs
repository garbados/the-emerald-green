(ns the-emerald-green.web.templates.deck
  (:require
   ["marked" :as marked]
   [clojure.string :as string]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.web.alchemy :refer [profane]]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.utils :refer [refresh-node]]))

;; an obsessive little quest
(def the-order-of-things
  [:the-fool
   :the-two-of-swords
   :the-two-of-wands
   :the-two-of-cups
   :the-two-of-pentacles
   :the-magician
   :the-high-priestess
   :the-three-of-swords
   :the-three-of-wands
   :the-three-of-cups
   :the-three-of-pentacles
   :the-empress
   :the-emperor
   :the-four-of-swords
   :the-four-of-wands
   :the-four-of-cups
   :the-four-of-pentacles
   :the-hierophant
   :the-five-of-swords
   :the-five-of-wands
   :the-five-of-cups
   :the-five-of-pentacles
   :the-lovers
   :the-six-of-swords
   :the-six-of-wands
   :the-six-of-cups
   :the-six-of-pentacles
   :the-chariot
   :strength
   :the-seven-of-swords
   :the-seven-of-wands
   :the-seven-of-cups
   :the-seven-of-pentacles
   :the-hermit
   :the-eight-of-wands
   :the-eight-of-cups
   :the-eight-of-swords
   :the-eight-of-pentacles
   :the-hanged-man
   :death
   :the-nine-of-wands
   :the-nine-of-cups
   :the-nine-of-swords
   :the-nine-of-pentacles
   :temperance
   :the-ten-of-swords
   :the-ten-of-wands
   :the-ten-of-cups
   :the-ten-of-pentacles
   :the-devil
   :the-tower
   :the-page-of-swords
   :the-page-of-wands
   :the-page-of-cups
   :the-page-of-pentacles
   :wheel-of-fortune
   :the-knight-of-swords
   :the-knight-of-wands
   :the-knight-of-cups
   :the-knight-of-pentacles
   :justice
   :the-star
   :the-queen-of-swords
   :the-queen-of-wands
   :the-queen-of-cups
   :the-queen-of-pentacles
   :the-moon
   :the-king-of-swords
   :the-king-of-wands
   :the-king-of-cups
   :the-king-of-pentacles
   :the-sun
   :judgement
   :the-ace-of-swords
   :the-ace-of-wands
   :the-ace-of-cups
   :the-ace-of-pentacles
   :the-world])

(defn trait-mentions-card?
  [{card-id :id :keys [tags] :as card}
   {card-req :card deck-req :deck}]
  (or (if (keyword? card-req)
        (contains? tags card-req)
        (contains? (set (flatten card-req)) card-id))
      (if (keyword? deck-req)
        (contains? tags deck-req)
        (contains? (set (flatten deck-req)) card-id))
      (when card-req
        (traits/rule-matches-card? card-req card))
      (when deck-req
        (traits/rule-matches-cards? deck-req #{card}))))

(def card->traits
  (reduce
   (fn [acc card]
     (let [traits (filter (partial trait-mentions-card? card) traits/traits)]
       (cond-> acc
         (seq traits) (assoc (:id card) (sort-by :name traits)))))
   {}
   deck/base-deck))

(def describe-card
  (memoize
   (fn [{card-id :id
         card-name :name
         :keys [tags]}]
     (let [{:keys [description media-description media-src]} (deck/card-metadata card-id)]
       [:div.box
        [:div.columns
         (when (seq media-src)
           [:div.column.is-3
            [:figure.image
             [:img {:alt media-description
                    :title media-description
                    :src media-src}]]])
         [:div.column
          [:h3 card-name]
          (when (seq description)
            (let [lines (string/split description #"\n")
                  poem (str "> " (string/join "  \n> " lines))]
              (profane "p" (marked/parse poem))))
          [:p "Tags:"]
          [:pre>code (with-out-str (println tags))]
          (when-let [traits (seq (card->traits card-id))]
            [:div
             [:p "Found in these traits:"]
             [:ul
              (for [{trait-name :name
                     :keys [description]} traits]
                [:li [:strong trait-name] ": " description])]])]]]))))

(defn card-matches-re?
  [{card-name :name
    :keys [description tags]}
   re]
  (or (re-find re card-name)
      (re-find re description)
      (re-find re (string/join ", " tags))))

(defn list-cards [query]
  (for [card-id the-order-of-things
        :let [card (deck/id->card card-id)
              re (re-pattern query)]
        :when (card-matches-re? card re)]
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
