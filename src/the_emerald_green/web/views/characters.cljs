(ns the-emerald-green.web.views.characters
  (:require
   [the-emerald-green.characters :as c]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.help :as help]
   [the-emerald-green.money :as money]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.utils :refer [keyword->name]]
   [the-emerald-green.web.db :refer [save-character!]]
   [the-emerald-green.web.routing :refer [goto route-pattern]]
   [the-emerald-green.web.templates.characters :as ct :refer [character-not-found]]
   [the-emerald-green.web.templates.equipment :refer [summarize-thing]]
   [the-emerald-green.web.utils :refer [atomify marshal-thing refresh-node]]))

(def sanctify-msg "Will you hold it sacred?")
(def nothing-sacred "Is nothing sacred to you?")
(def exile-msg "Do you have no need of it?")
(def nothing-exiled "Unburden yourself...")
(def restore-msg "Will you lower it to the real?")

(defn on-choice [maximum -choices confirm-msg]
  (when (< (count @-choices) maximum)
    #(when (js/confirm (str (-> % deck/id->card :name) "... " confirm-msg))
       (swap! -choices conj %))))

(defn on-sanctify [level -sanctified]
  (on-choice (c/max-sanctified level) -sanctified sanctify-msg))

(defn on-exile [level -exiled]
  (on-choice (c/max-exiled level) -exiled exile-msg))

(defn list-chosen [empty-msg -chosen]
  (let [on-restore
        #(when (js/confirm (str (:name %) "... " restore-msg))
           (swap! -chosen disj (:id %)))]
    #(ct/list-chosen @-chosen
                     :on-restore on-restore
                     :empty-msg empty-msg)))

(defn list-stuff [type->stuff & {:keys [can-buy on-buy on-get on-sell on-lose]}]
  (for [stuff-type equipment/stuff-types
        :let [title (equipment/type->title stuff-type)
              stuff (filter
                     #(and
                       (false? (:abstract %))
                       (#{:common :uncommon} (:rarity %)))
                     (type->stuff stuff-type []))]
        :when (seq stuff)]
    [:div.block
     [:h3.subtitle title]
     [:table.table.is-hoverable.is-fullwidth
      [:thead
       [:tr
        [:th.is-fullwidth "Name"]
        (when on-buy
          [:th.is-narrow.has-text-centered "Buy?"])
        (when on-get
          [:th.is-narrow.has-text-centered "Get?"])
        (when on-sell
          [:th.is-narrow.has-text-centered "Sell?"])
        (when on-lose
          [:th.is-narrow.has-text-centered "Lose?"])]]
      [:tbody
       (for [thing stuff]
         [:tr
          [:td [:p (help/->title (summarize-thing thing)) (:name thing)]]
          (when on-buy
            [:td [:button.button.is-primary.is-small
                  (if (can-buy thing)
                    {:title (str "Spend " (:cost thing) " to purchase!")
                     :onclick #(on-buy thing)}
                    {:title "Too expensive..."
                     :disabled true})
                  "Buy!"]])
          (when on-get
            [:td [:button.button.is-info.is-small
                  {:title "Obtain for free!"
                   :onclick #(on-get thing)}
                  "Get!"]])
          (when on-sell
            [:td [:button.button.is-warning.is-small
                  {:title "Pawn it for gold!"
                   :onclick #(on-sell thing)}
                  "Sell!"]])
          (when on-lose
            [:td [:button.button.is-danger.is-small
                  {:title "Lose it, gain nothing!"
                   :onclick #(on-lose thing)}
                  "Lose!"]])])]]]))

(defn edit-character [type->stuff & {:keys [character on-save id]}]
  (let [{:keys [-name -biography -level -sanctified -exiled -wealth -equipped]
         :as atoms}
        (atomify character
                 :name ""
                 :biography ""
                 :level 1
                 :sanctified #{}
                 :exiled #{}
                 :equipped (zipmap equipment/stuff-types (repeat []))
                 :wealth 1000)
        -traits (atom
                 (when-let [pact (seq (into @-exiled @-sanctified))]
                   (traits/determine-traits pact)))
        -deck-query (atom "")
        -shop-query (atom "")
        check-thing
        (fn [thing]
          (let [re (re-pattern @-shop-query)]
            (or (re-find re (:name thing))
                (re-find re (:description thing))
                (re-find re (:type thing))
                (not-empty
                 (for [tag (:tags thing [])
                       :when (re-find re (name tag))]
                   true)))))
        save-character
        #(save-character! (marshal-thing atoms) id)
        list-own-deck
        #(ct/filter-deck
          (fn [card]
            (c/card-available? card
                               :sanctified @-sanctified
                               :exiled @-exiled
                               :query @-deck-query))
          :on-exile (on-exile @-level -exiled)
          :on-sanctify (on-sanctify @-level -sanctified))
        list-sanctified (list-chosen nothing-sacred -sanctified)
        list-exiled (list-chosen nothing-exiled -exiled)
        list-own-traits #(ct/list-traits @-traits)
        list-own-stats #(ct/list-stats-from-traits @-level @-traits)
        show-wealth #(money/wealth-to-gold @-wealth)
        list-equipped
        #(list-stuff @-equipped
                     :on-sell
                     (fn [thing]
                       (when (js/confirm (str "Are you sure you want to sell " (:name thing) " for " (:cost thing) "?"))
                         (swap! -equipped update (:type thing) (partial remove (partial = thing)))
                         (reset! -wealth (+ @-wealth (money/gold-to-wealth (:cost thing))))))
                     :on-lose
                     (fn [thing]
                       (when (js/confirm (str "Are you sure you want to lose " (:name thing) " for nothing?"))
                         (swap! -equipped update (:type thing) (partial remove (partial = thing))))))
        list-shopping
        #(list-stuff
          (reduce
           (fn [acc [stuff-type stuff]]
             (assoc acc stuff-type (filter check-thing stuff)))
           {}
           type->stuff)
          :can-buy
          (fn [thing]
            (<= (money/gold-to-wealth (:cost thing)) @-wealth))
          :on-buy
          (fn [thing]
            (when (js/confirm (str "Are you sure you want to buy " (:name thing) " for " (:cost thing) "?"))
              (swap! -equipped update (:type thing) conj thing)
              (reset! -wealth (- @-wealth (money/gold-to-wealth (:cost thing))))))
          :on-get
          (fn [thing]
            (when (js/confirm (str "Are you sure you want to get " (:name thing) " for free?"))
              (swap! -equipped update (:type thing) conj thing))))
        reset-traits #(reset! -traits
                              (when-let [pact (seq (into @-exiled @-sanctified))]
                                (traits/determine-traits pact)))
        refresh-shopping (partial refresh-node "shopping" list-shopping)
        refresh-equipped (partial refresh-node "equipped" list-equipped)
        refresh-wealth (partial refresh-node "wealth" show-wealth)
        refresh-deck   (partial refresh-node "deck" list-own-deck)
        refresh-traits (partial refresh-node "traits" list-own-traits)
        refresh-stats  (partial refresh-node "stats" list-own-stats)
        refresh-sanctified (partial refresh-node "sanctified" list-sanctified)
        refresh-exiled (partial refresh-node "exiled" list-exiled)]
    (add-watch -sanctified :sanctify
               #(do (refresh-deck)
                    (reset-traits)
                    (refresh-sanctified)))
    (add-watch -exiled :exiled
               #(do (refresh-deck)
                    (reset-traits)
                    (refresh-exiled)))
    (add-watch -traits :traits
               #(do (refresh-traits)
                    (refresh-stats)))
    (add-watch -deck-query :query refresh-deck)
    (add-watch -shop-query :query refresh-shopping)
    (add-watch -equipped :equipped refresh-equipped)
    (add-watch -wealth :gold #(do (refresh-wealth)
                                  (refresh-shopping)))
    (ct/edit-character
     -name
     -biography
     -deck-query
     -shop-query
     :deck (list-own-deck)
     :traits (list-own-traits)
     :stats (list-own-stats)
     :exiled (list-exiled)
     :sanctified (list-sanctified)
     :new? (nil? character)
     :equipped (list-equipped)
     :shopping (list-shopping)
     :wealth (show-wealth)
     :on-save
     (when on-save
       #(.then (save-character)
               (fn [js-res]
                 (on-save (.-id js-res))))))))

(defn show-character [custom-characters]
  (let [character-ref (route-pattern :show-character)
        character
        (get custom-characters character-ref (c/id->example (keyword character-ref)))]
    (if character
      (ct/show-character character)
      (character-not-found custom-characters character-ref :show? true))))

(defn new-character [custom-stuff]
  (edit-character
   (equipment/merge-custom-stuff custom-stuff)
   :on-save #(goto :show-character %)))

(defn template-character [custom-characters custom-stuff]
  (let [example-ref (route-pattern :template-character)
        example-id (keyword example-ref)
        character (first (filter #(= example-id (:id %)) c/examples))]
    (if character
      (edit-character
       (equipment/merge-custom-stuff custom-stuff)
       :character character
                      :on-save #(goto :show-character %))
      (character-not-found custom-characters example-ref))))

(defn edit-custom-character [custom-characters custom-stuff]
  (let [character-ref (route-pattern :edit-character)
        character (get custom-characters character-ref)]
    (if character
      (edit-character
       (equipment/merge-custom-stuff custom-stuff)
       :character character
       :on-save #(goto :show-character %)
       :id character-ref)
      (character-not-found custom-characters character-ref))))

(defn list-characters [custom-characters]
  [[:h1.title "Characters"]
   (ct/list-characters custom-characters :show? true)])
