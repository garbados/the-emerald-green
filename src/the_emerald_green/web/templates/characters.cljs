(ns the-emerald-green.web.templates.characters
  (:require
   ["marked" :as marked]
   [clojure.string :as string]
   [the-emerald-green.characters :as c]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.utils :refer [s-format]]
   [the-emerald-green.web.alchemy :refer [profane]]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.templates.traits :as ct]
   [the-emerald-green.web.utils :refer [refresh-node]]))

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
    (cons :tbody
          (for [card cards]
            [:tr
             [:td (:name card)]
             [:td [:button.button.is-small.is-danger
                   (if on-exile
                     {:onclick (partial on-exile card)}
                     {:disabled true})
                   "Exile!"]]
             [:td [:button.button.is-small.is-info
                   (if on-sanctify
                     {:onclick (partial on-sanctify card)}
                     {:disabled true})
                   "Sanctify!"]]]))]])

(defn list-chosen [cards & {:keys [on-restore empty-msg]}]
  (if (seq cards)
    [:table.table.is-hoverable.is-fullwidth
     [:thead
      [:tr
       [:th.is-fullwidth "Name"]
       [:th.is-narrow "Restore?"]]]
     (cons :tbody
           (for [card cards]
             [:tr
              [:td (:name card)]
              [:td [:button.button.is-small.is-dark
                    (if on-restore
                      {:onclick (partial on-restore card)}
                      {:disabled true})
                    "Restore!"]]]))]
    [:p "None. " [:em empty-msg]]))

(defn list-stats
  [{:keys [attributes skills talents abilities]}
   fungibles]
  [[:p.subtitle "Attributes"]
   [:table.table.is-fullwidth
    [:thead
     [:tr
      [:th "Body"]
      [:th "Mind"]
      [:th "Spirit"]
      [:th "Luck"]]]
    [:tbody
     [:tr
      [:td (:body attributes)]
      [:td (:mind attributes)]
      [:td (:spirit attributes)]
      [:td (:luck attributes)]]]]
   [:p.subtitle "Fungibles"]
   [:table.table.is-fullwidth
    [:thead
     [:tr
      [:th "Health"]
      [:th "Draw"]
      [:th "Will"]
      [:th "Fortune"]
      [:th "Madness"]]]
    [:tbody
     [:tr
      [:td (:health fungibles)]
      [:td (:draw fungibles)]
      [:td (:will fungibles)]
      [:td (:fortune fungibles)]
      [:td (:madness fungibles)]]]]
   (when-let [known-skills (seq (map first (filter second skills)))]
     [:div
      [:hr]
      [:p.subtitle "Skills"]
      (cons :ul
            (for [skill known-skills]
              [:li (string/capitalize (name skill))]))])
   (when (seq talents)
     [:div
      [:hr]
      [:p.subtitle "Talents"]
      (cons :ul
            (for [talent talents]
              [:li (print-str talent)]))])
   (when (seq abilities)
     [:div
      [:hr]
      [:p.subtitle "Abilities"]
      (if (seq abilities)
        (cons :ul
              (for [ability abilities]
                [:li (print-str ability)]))
        [:p "No abilities..."])])])

(defn list-traits [traits & {:keys [height]
                             :or {height default-height}}]
  [:div
   {:style (str "overflow: scroll; max-height: " height "px;")}
   (for [[trait-id n] traits
         :let [trait (traits/id->trait trait-id)]]
     (ct/describe-trait trait n))])

(defn edit-character [& {:keys [character on-save on-cancel]}]
  (let [{char-name :name
         :keys [biography sanctified exiled level]
         :or {level 1
              char-name ""
              biography ""
              sanctified #{}
              exiled #{}}} character
        -name (atom char-name)
        -biography (atom biography)
        -sanctified (atom sanctified)
        -exiled (atom exiled)
        -traits (atom nil)
        -query (atom "")
        on-sanctify
        #(when (> (c/max-sanctified level) (count @-sanctified))
           (fn [card]
             (when (js/confirm (str (:name card) "... Will you hold it sacred?"))
               (swap! -sanctified conj card))))
        on-exile
        #(when (> (c/max-exiled level) (count @-exiled))
           (fn [card]
             (when (js/confirm (str (:name card) "... Will you banish it?"))
               (swap! -exiled conj card))))
        list-own-deck
        #(list-cards
          (remove
           (fn [card]
             (or (contains? (into @-sanctified @-exiled) card)
                 (and (seq @-query)
                      (empty?
                       (filter (partial re-find (re-pattern @-query))
                               (map name (:tags card)))))))
           deck/the-ordered-deck)
          :on-exile (on-exile)
          :on-sanctify (on-sanctify))
        list-sanctified
        #(list-chosen @-sanctified
                      :on-restore
                      (fn [card]
                        (swap! -sanctified disj card))
                      :empty-msg
                      "Is nothing sacred to you?")
        list-exiled
        #(list-chosen @-exiled
                      :on-restore
                      (fn [card]
                        (swap! -exiled disj card))
                      :empty-msg
                      "Unburden yourself...")
        list-own-traits #(list-traits @-traits)
        list-own-stats
        #(let [{:keys [attributes skills] :as stats}
               (c/determine-stats @-traits)
               fungibles
               (c/reset-fungibles {:attributes attributes
                                   :skills skills
                                   :level level})]
           (list-stats stats fungibles))
        reset-traits #(reset! -traits (traits/determine-traits
                                       (into (set (map :id @-exiled))
                                             (set (map :id @-sanctified)))))
        refresh-deck #(refresh-node "deck" list-own-deck)
        refresh-traits #(refresh-node "traits" list-own-traits)
        refresh-stats #(refresh-node "stats" list-own-stats)]
    (add-watch -sanctified :sanctify
               #(do (refresh-deck)
                    (reset-traits)
                    (refresh-node "sanctified" list-sanctified)))
    (add-watch -exiled :exiled
               #(do (refresh-deck)
                    (reset-traits)
                    (refresh-node "exiled" list-exiled)))
    (add-watch -traits :traits
               #(do (refresh-traits)
                    (refresh-stats)))
    (add-watch -query :query refresh-deck)
    [:div.content
     [:h1 (if character "Edit Character" "New Character")]
     [:div.field
      [:label.label "Name"]
      [:div.control
       (prompts/text -name)]]
     [:div.field
      [:label.label "Biography"]
      [:div.control
       (prompts/textarea -biography)]]
     [:div.columns
      [:div.column.is-6
       [:div.box
        [:p.title "Deck"]
        (prompts/text -query
                      :placeholder "🔍 Filter cards by name or tag.")
        [:hr]
        [:div#deck (list-own-deck)]]
       [:div.box
        [:h1.title "Traits"]
        [:div#traits (list-own-traits)]]]
      [:div.column.is-6
       [:div.box
        [:p.title "Sanctified"]
        [:div#sanctified (list-sanctified)]
        [:hr]
        [:p.title "Exiled"]
        [:div#exiled (list-exiled)]]
       [:div.box
        [:h1.title "Stats"]
        [:div#stats (list-own-stats)]]]]
     (when (or on-save on-cancel)
       [:div.buttons
        (when on-save
          [:button.button.is-primary.is-fullwidth "Save"])
        (when on-cancel
          [:button.button.is-light.is-outlined.is-fullwidth "Cancel"])])]))

(def new-character edit-character) ; yay optional arguments!

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
          card-name]))])])

(defn list-characters
  ([] (list-characters []))
  ([characters]
   [:div.content
    [:h1 "Characters"]
    (for [character (concat characters c/examples)]
      [:details
       [:summary (s-format "%s < %s >" (:name character) (:level character))]
       (show-character character)])]))
