(ns the-emerald-green.web.templates.tools
  (:require
   [the-emerald-green.web.alchemy :refer [snag alchemize]]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.character :as c]
   [the-emerald-green.web.templates.guides :as guides]
   [clojure.string :as string]))

(defn list-cards [cards & {:keys [on-exile on-sanctify height]
                           :or {height "300px"}}]
  [:div
   {:style (str "overflow: scroll; height: " height ";")}
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

(defn list-stats [{:keys [attributes skills talents abilities]} fungibles]
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
      [:th "Will"]
      [:th "Fortune"]
      [:th "Madness"]]]
    [:tbody
     [:tr
      [:td (:health fungibles)]
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

(defn edit-character [& {:keys [character on-save on-cancel]}]
  (let [{charname :name
         :keys [biography sanctified exiled level]
         :or {level 1
              charname ""
              biography ""
              sanctified #{}
              exiled #{}}} character
        -name (atom charname)
        -biography (atom biography)
        -sanctified (atom sanctified)
        -exiled (atom exiled)
        -query (atom "")
        on-sanctify
        #(when (> 2 (count @-sanctified))
           (fn [card]
             (when (js/confirm (str (:name card) "... Will you hold it sacred?"))
               (swap! -sanctified conj card))))
        on-exile
        #(when (> 3 (count @-exiled))
           (fn [card]
             (when (js/confirm (str (:name card) "... Will you banish it?"))
               (swap! -exiled conj card))))
        list-deck
        #(list-cards
          (remove
           (fn [card]
             (or (contains? (into @-sanctified @-exiled) card)
                 (and (seq @-query)
                      (empty?
                       (filter (partial re-find (re-pattern @-query))
                               (map name (:tags card)))))))
           deck/ordered-base-deck)
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
        list-traits
        #(->> {:sanctified (set (map :id @-sanctified))
               :exiled (set (map :id @-exiled))}
              (c/determine-traits)
              (group-by identity)
              (map (juxt first (comp count second)))
              (sort-by (comp :name first))
              (map
               (fn [[trait n]]
                 (guides/print-trait trait n))))
        list-stats
        #(let [{:keys [attributes skills] :as stats}
               (->> {:sanctified (set (map :id @-sanctified))
                     :exiled (set (map :id @-exiled))}
                    (c/determine-traits)
                    (c/determine-stats))
               fungibles
               (c/reset-fungibles {:attributes attributes
                                   :skills skills
                                   :level level})]
           (list-stats stats fungibles))
        refresh-node
        (fn [node-id template-fn]
          (.replaceChildren (snag node-id)
                            (alchemize (template-fn))))
        refresh-deck #(refresh-node "deck" list-deck)
        refresh-traits #(refresh-node "traits" list-traits)
        refresh-stats #(refresh-node "stats" list-stats)
        refresh-sanctified
        #(do (refresh-deck)
             (refresh-traits)
             (refresh-stats)
             (refresh-node "sanctified" list-sanctified))
        refresh-exiled
        #(do (refresh-deck)
             (refresh-traits)
             (refresh-stats)
             (refresh-node "exiled" list-exiled))]
    (add-watch -sanctified :sanctify refresh-sanctified)
    (add-watch -exiled :exiled refresh-exiled)
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
                      :placeholder "Filter cards by name or tag")
        [:hr]
        [:div#deck (list-deck)]]]
      [:div.column.is-6
       [:div.box
        [:p.title "Sanctified"]
        [:div#sanctified (list-sanctified)]
        [:hr]
        [:p.title "Exiled"]
        [:div#exiled (list-exiled)]]]]
     [:div.columns
      [:div.column.is-6
       [:div.box
        [:h1.title "Traits"]
        [:div#traits (list-traits)]]]
      [:div.column.is-6
       [:div.box
        [:h1.title "Stats"]
        [:div#stats (list-stats)]]]]
     (when (or on-save on-cancel)
       [:div.buttons
        (when on-save
          [:button.button.is-primary.is-fullwidth "Save"])
        (when on-cancel
          [:button.button.is-light.is-outlined.is-fullwidth "Cancel"])])]))

(def new-character edit-character) ; yay optional arguments!

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
