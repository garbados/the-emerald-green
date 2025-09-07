(ns the-emerald-green.web.templates.characters
  (:require
   ["marked" :as marked]
   [clojure.string :as string]
   [the-emerald-green.characters :as c]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.help :as help :refer [markdown-tip]]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.utils :refer [keyname keyword->name s-format]]
   [the-emerald-green.web.alchemy :refer [profane]]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.routing :refer [route->href]]
   [the-emerald-green.web.templates.traits :as ct]))

(def default-height 500)

(defn list-cards [cards & {:keys [on-exile on-sanctify height]
                           :or {height default-height}}]
  [:div
   {:style (str "overflow: scroll; max-height: " height "px;")}
   [:table.table.is-hoverable.is-fullwidth
    [:thead
     [:tr
      [:th.is-fullwidth "Name"]
      [:th.is-narrow "Exile?"]
      [:th.is-narrow "Sanctify?"]]]
    [:tbody
     (for [card-id cards
           :let [{card-name :name
                  :as card} (deck/id->card card-id)]]
       [:tr
        [:td (help/tag->title card) card-name]
        [:td [:button.button.is-small.is-danger
              (if on-exile
                {:onclick (partial on-exile card-id)}
                {:disabled true
                 :title "You despise too much..."})
              "Exile!"]]
        [:td [:button.button.is-small.is-info
              (if on-sanctify
                {:onclick (partial on-sanctify card-id)}
                {:disabled true
                 :title "You hold too much dear..."})
              "Sanctify!"]]])]]])

(defn list-chosen [cards & {:keys [on-restore empty-msg]}]
  (if (seq cards)
    [:table.table.is-hoverable.is-fullwidth
     [:thead
      [:tr
       [:th.is-fullwidth "Name"]
       [:th.is-narrow "Restore?"]]]
     [:tbody
      (for [card-id cards
            :let [{card-name :name
                   :as card} (deck/id->card card-id)]]
        [:tr
         [:td (help/tag->title card) card-name]
         [:td [:button.button.is-small.is-dark
               (if on-restore
                 {:onclick (partial on-restore card)}
                 {:disabled true})
               "Restore!"]]])]]
    [:p "None. " (when empty-msg [:em empty-msg])]))

(defn list-stats
  [{:keys [attributes skills talents abilities]}
   fungibles]
  [[:p.subtitle "Attributes"]
   [:table.table.is-fullwidth
    [:thead
     [:tr
      (for [attr c/attr-order]
        [:th (help/tag->title attr) (keyword->name attr)])]]
    [:tbody
     [:tr
      (for [tag c/attr-order]
        [:td (get attributes tag)])]]]
   [:p.subtitle "Fungibles"]
   [:table.table.is-fullwidth
    [:thead
     [:tr
      (for [attr c/fung-order]
        [:th (help/tag->title attr) (keyword->name attr)])]]
    [:tbody
     [:tr
      (for [tag c/fung-order]
        [:td (get fungibles tag)])]]]
   (when-let [known-skills (seq (map first (filter second skills)))]
     [:div
      [:hr]
      [:p.subtitle "Skills"]
      [:ul
       (for [skill known-skills]
         [:li (help/tag->title skill) (string/capitalize (name skill))])]])
   [:div
    [:hr]
    [:p.subtitle "Talents"]
    (if (seq talents)
      [:ul
       (for [talent talents]
         [:li (print-str talent)])]
      [:p "No talents..."])]
   [:div
    [:hr]
    [:p.subtitle "Abilities"]
    (if (seq abilities)
      [:ul
       (for [ability-id abilities]
         [:li (print-str ability-id)])]
      [:p "No abilities..."])]])

(defn list-stats-from-traits [level traits]
  (let [{:keys [attributes skills] :as stats}
        (c/determine-stats traits)
        fungibles
        (c/reset-fungibles {:attributes attributes
                            :skills skills
                            :level level})]
    (list-stats stats fungibles)))

(defn list-traits [traits & {:keys [height]
                             :or {height default-height}}]
  [:div
   {:style (str "overflow: scroll; max-height: " height "px;")}
   (if (seq traits)
     (for [[trait-id n] traits
           :let [trait (traits/id->trait trait-id)]]
       (ct/describe-trait trait n))
     [:p "No traits..."])])

(defn filter-deck [filter-fn & args]
  (apply list-cards (map :id (remove filter-fn deck/the-ordered-deck)) args))

(defn edit-character
  [-name
   -biography
   -deck-query
   -shop-query
   & {:keys [new? on-save on-cancel
             deck traits sanctified exiled stats
             weapons armor tools consumables items]}]
  [:div.content
   [:h1 (if new? "New Character" "Edit Character")]
   [:div.field
    [:label.label "Name"]
    [:div.control (prompts/text -name)]
    [:p.help "What do you call yourself? What would you have others call you?"]]
   [:div.field
    [:label.label "Biography"]
    [:div.control (prompts/textarea -biography)]
    [:p.help "What's your story? " markdown-tip]]
   [:h2 "The Pact"]
   [:div.columns
    [:div.column.is-6
     [:div.box
      [:p.subtitle "Deck"]
      (prompts/text -deck-query :placeholder "🔍 Filter cards by name or tag.")
      [:hr]
      [:div#deck deck]]
     [:div.box
      [:p.subtitle "Traits"]
      [:div#traits traits]]]
    [:div.column.is-6
     [:div.box
      [:p.subtitle "Sanctified"]
      [:div#sanctified sanctified]
      [:hr]
      [:p.subtitle "Exiled"]
      [:div#exiled exiled]]
     [:div.box
      [:p.subtitle "Stats"]
      [:div#stats stats]]]]
   [:h2 "The Livery"]
   [:div.columns
    [:div.column.is-6
     [:div.box
      [:p.subtitle "Shopping"]
      (prompts/text -shop-query :placeholder "🔍 Filter stuff by name or attributes.")
      [:div#weapons weapons]
      [:div#armor armor]
      [:div#consumables consumables]
      [:div#tools tools]
      [:div#items items]]]
    [:div.column.is-6
     [:div.box
      [:p.subtitle "Equipment"]]
     [:div.box
      [:p.subtitle "Inventory"]]]]
   (when (or on-save on-cancel)
     [:div.buttons
      (when on-save
        [:button.button.is-primary.is-fullwidth "Save"])
      (when on-cancel
        [:button.button.is-light.is-outlined.is-fullwidth "Cancel"])])])

(defn show-character [character]
  [:div.box
   [:h3 (:name character)]
   (profane "p" (marked/parse (:biography character)))
   [:h5 "Pact"]
   (for [group [:sanctified :exiled]
         :let [group-name (-> group name string/capitalize)
               cards (get character group)]]
     [:p group-name ": "
      (interpose
       ", "
       (for [card-id cards
             :let [{card-name :name
                    :keys [description]} (deck/id->card card-id)]]
         [:span
          (cond-> {}
            (seq description)
            (merge {:title description
                    :style "cursor: help; text-decoration: underline dotted;"}))
          card-name]))])
   [:h5 "Livery"]
   [:div.buttons
    (when (:id character)
      [:a.button.is-fullwidth (route->href :template-character (-> character :id keyname)) "Use as Template"])
    (when-let [_id (:_id character)]
      [:a.button.is-fullwidth (route->href :edit-character _id) "Edit"])]])

(defn list-characters
  ([] (list-characters []))
  ([characters]
   [:div.content
    [:h1 "Characters"]
    (for [character (concat characters c/examples)]
      [:details
       [:summary (s-format "%s < %s >" (:name character) (:level character))]
       (show-character character)])]))
