(ns the-emerald-green.web.templates.deck
  (:require
   ["marked" :as marked]
   [clojure.string :as string]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.utils :refer [keyword->name]]
   [the-emerald-green.web.alchemy :refer [profane]]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.utils :refer [pprint refresh-node]]
   [the-emerald-green.help :as help]))

(defn trait-mentions-card?
  [{card-id :id :keys [tags] :as card}
   {card-reqs :card deck-reqs :deck}]
  (and
   ; ignore traits that use "not" because it's complicated
   (nil?
    (->> [card-reqs deck-reqs]
         (map #(if (keyword? %) % (first %)))
         (filter (partial = :not))
         seq))
   (or (if (keyword? card-reqs)
         (contains? tags card-reqs)
         (contains? (set (flatten card-reqs)) card-id))
       (if (keyword? deck-reqs)
         (contains? tags deck-reqs)
         (contains? (set (flatten deck-reqs)) card-id))
       (when card-reqs
         (traits/rule-matches-card? card-reqs card))
       (when deck-reqs
         (traits/rule-matches-cards? deck-reqs #{card})))))

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
         :keys [description tags]}]
     (let [{:keys [media-description media-src]} (deck/id->metadata card-id)]
       [:div.block>div.box
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
          [:p "Tags: "
           (interpose
            ", "
            (for [tag (sort-by name tags)
                  :let [tag-name (keyword->name tag)]]
              [:span (help/tag->title tag) tag-name]))]
          (when-let [traits (seq (card->traits card-id))]
            [:div
             [:p "Found in these traits:"]
             [:ul
              (for [{trait-name :name
                     :keys [description]} traits]
                [:li [:strong trait-name] ": " description])]])]]]))))

(defn list-cards [query]
  (for [card-id deck/the-order-of-things
        :let [card (deck/id->card card-id)
              re (re-pattern query)]
        :when (deck/card-matches-re? card re)]
    (describe-card card)))

(defn card-guide []
  (let [-query (atom "")
        refresh-cards #(refresh-node "cards" (partial list-cards @-query))]
    (add-watch -query :query refresh-cards)
    [:div.content
     [:h1 "Card Guide"]
     [:div.block
      [:p "A 78-card Tarot deck has the following cards. They tell a kind of story, and contain many fates."]
      (prompts/text -query
                    :placeholder "🔍 Filter cards by name, description, or requirements.")]
     [:div#cards (list-cards @-query)]]))
