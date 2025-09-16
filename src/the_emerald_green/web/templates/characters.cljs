(ns the-emerald-green.web.templates.characters
  (:require
   ["marked" :as marked]
   [clojure.string :as string]
   [the-emerald-green.characters :as c]
   [the-emerald-green.core :as core]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.help :as help :refer [markdown-tip]]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.utils :refer [keyname keyword->name]]
   [the-emerald-green.web.alchemy :refer [profane]]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.routing :refer [route->href]]
   [the-emerald-green.web.templates.traits :as ct :refer [describe-ability
                                                          describe-talent]]))

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
                 :title "You cherish too much..."})
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

(defn describe-attributes [attributes]
  [:div.box>div.block
   [:p.subtitle "Attributes"]
   [:table.table.is-fullwidth
    [:thead
     [:tr
      (for [attr core/attr-order]
        [:th (help/tag->title attr) (keyword->name attr)])]]
    [:tbody
     [:tr
      (for [tag core/attr-order]
        [:td (get attributes tag)])]]]])

(defn describe-fungibles [fungibles]
  [:div.box>div.block
   [:p.subtitle "Fungibles"]
   [:table.table.is-fullwidth
    [:thead
     [:tr
      (for [fung core/fung-order]
        [:th (help/tag->title fung) (keyword->name fung)])]]
    [:tbody
     [:tr
      (for [fung core/fung-order]
        [:td (get fungibles fung)])]]]])

(defn list-stats
  [{:keys [attributes skills talents abilities]}
   fungibles]
  [:div.block
   (describe-attributes attributes)
   (describe-fungibles fungibles)
   (when-let [known-skills (seq (map first (filter second skills)))]
     [:div.box>div.block
      [:p.subtitle "Skills"]
      [:ul
       (for [skill known-skills]
         [:li (help/tag->title skill) (string/capitalize (name skill))])]])
   (when (seq talents)
     [:div.box>div.block
      [:p.subtitle "Talents"]
      (for [talent talents]
        (describe-talent talent))])
   (when (seq abilities)
     [:div.box>div.block
      [:p.subtitle "Abilities"]
      (for [ability-id abilities]
        (describe-ability ability-id))])])

(defn list-stats-from-traits [level traits]
  (let [{:keys [attributes skills] :as stats}
        (c/determine-stats traits)
        fungibles
        (c/reset-fungibles {:attributes attributes
                            :skills skills
                            :level level})]
    (list-stats stats fungibles)))

(defn list-traits [traits]
  [:div.block
   (if (seq traits)
     (for [[trait-id n] (sort-by (comp name first) traits)
           :let [trait (traits/id->trait trait-id)]]
       [:details
        [:summary
         (if (< 1 n)
           (str (:name trait) " x" n)
           (:name trait))]
        [:br]
        (if (< 1 n)
          (ct/describe-trait trait n)
          (ct/describe-a-trait trait))
        [:br]])
     [:p "No traits..."])])

(defn filter-deck [filter-fn & args]
  (apply list-cards (map :id (remove filter-fn deck/the-ordered-deck)) args))

(defn edit-character
  [-name
   -biography
   -deck-query
   -shop-query
   & {:keys [new? on-save
             deck traits sanctified exiled stats
             equipment]}]
  [[:h1.title (if new? "New Character" "Edit Character")]
   [:div.block
    [:div.field
     [:label.label "Name"]
     [:div.control (prompts/text -name)]
     [:p.help "What do you call yourself? What would you have others call you?"]]
    [:div.field
     [:label.label "Biography"]
     [:div.control (prompts/textarea -biography)]
     [:p.help "What's your story? " markdown-tip]]]
   [:div.block
    [:h2.subtitle "The Pact"]
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
       [:p.subtitle (help/tag->title :sanctified) "Sanctified"]
       [:div#sanctified sanctified]
       [:hr]
       [:p.subtitle (help/tag->title :exiled) "Exiled"]
       [:div#exiled exiled]]
      [:div.box
       [:p.subtitle "Stats"]
       [:div#stats stats]]]]]
   [:div.block
    [:h2.subtitle "The Livery"]
    [:div.columns
     [:div.column.is-6
      [:div.box
       [:p.subtitle "Shopping"]
       (prompts/text -shop-query :placeholder "🔍 Filter stuff by name or attributes.")
       [:div#equipment equipment]]]
     [:div.column.is-6
      [:div.box
       [:p.subtitle "Equipment"]]
      [:div.box
       [:p.subtitle "Inventory"]]]]]
   (when on-save
     [:div.block
      [:button.button.is-primary.is-fullwidth
       {:onclick on-save}
       "Save"]])])

(defn show-character [{:as character
                       :keys [level traits]}
                      & {:keys [on-delete]}]
  [[:div.block
    [:div.level
     [:div.level-left
      [:div.level-item
       [:h3.subtitle (:name character)]]]
     [:div.level-right
      [:div.level-item
       [:div.buttons.has-addons
        (when (:id character)
          [:a.button.is-light (route->href :template-character (-> character :id keyname)) "Use as Template"])
        (when on-delete
          [:button.button.is-danger {:on-click on-delete} "Delete!"])
        (when-let [_id (:_id character)]
          [:a.button.is-info (route->href :edit-character _id) "Edit"])]]]]
    [:details
     [:summary "Biography"]
     (profane "blockquote" (marked/parse (:biography character)))]]
   [:div.block
    [:h4.subtitle "Pact"]
    [:div.box
     (for [group [:sanctified :exiled]
           :let [group-name (-> group name string/capitalize)
                 cards (get character group)]]
       [:p
        [:span (help/tag->title group) group-name]
        ": "
        (interpose
         ", "
         (for [card-id cards
               :let [{card-name :name
                      :keys [description]} (deck/id->card card-id)]]
           [:span (help/tag->title description) card-name]))])]]
   [:div.block
    [:h4.subtitle "Stats"]
    (list-stats-from-traits level traits)]
   [:div.block
    [:h4.subtitle "Traits"]
    (list-traits traits)]
   [:div.block
    [:h4.subtitle "Livery"]]])

(defn summarize-character [character & {:keys [show?]
                                        :or {show? false}}]
  (let [example? (keyword? (:id character))
        [route ref]
        (if show?
          [:show-character (:_id character (-> character :id keyname))]
          (if example?
            [:template-character (-> character :id keyname)]
            [:edit-character (:_id character)]))]
    [:div.box
     [:p.subtitle [:a (route->href route ref) (:name character)]]
     [:p
      {:style "max-height: 300px; overflow: scroll;"}
      (profane "blockquote" (marked/parse (:biography character)))]]))

(defn list-characters
  [characters & {:keys [show?]}]
  (let [summarize #(summarize-character % :show? show?)]
    [[:div.block
      [:p.subtitle "Examples"]
      (map summarize c/examples)]
     (when (seq characters)
       [:div.block
        [:p.subtitle "Custom"]
        (map summarize (vals characters))])]))

(defn character-not-found [custom-characters attempted-ref & {:keys [show?]}]
  (let [attempted-str (if (keyword? attempted-ref) (keyname attempted-ref) attempted-ref)
        example? (string/starts-with? attempted-ref "example")
        error-msg (str "No " (if example? "example" "custom") " character with this ID: " attempted-str)]
    [[:div.block
      [:h1.title "Character not found!"]]
     [:div.block
      [:p.subtitle error-msg]]
     [:div.block
      [:p "Why not " [:a (route->href :new-character) "make a new character"] "?"]
      [:p "Or use another character:"]]
     [:div.block
      (list-characters custom-characters :show? show?)]]))
