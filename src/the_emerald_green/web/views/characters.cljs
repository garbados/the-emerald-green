(ns the-emerald-green.web.views.characters
  (:require
   [the-emerald-green.characters :as c]
   [the-emerald-green.deck :as deck]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.web.db :as db]
   [the-emerald-green.web.routing :refer [goto route-pattern]]
   [the-emerald-green.web.templates.characters :as ct :refer [character-not-found]]
   [the-emerald-green.web.utils :refer [refresh-node]]))

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
        #(when (js/confirm (str (-> % deck/id->card :name) "... " restore-msg))
           (swap! -chosen disj %))]
    #(ct/list-chosen @-chosen
                     :on-restore on-restore
                     :empty-msg empty-msg)))

(defn edit-character [& {:keys [character on-save id]}]
  (let [{char-name :name
         :keys [biography sanctified exiled level wealth]
         :or {level 1
              char-name ""
              biography ""
              sanctified #{}
              exiled #{}
              wealth 1000}} character
        -name (atom char-name)
        -biography (atom biography)
        -level (atom level)
        -sanctified (atom sanctified)
        -exiled (atom exiled)
        -wealth (atom wealth)
        -traits (atom
                 (when-let [pact (seq (into @-exiled @-sanctified))]
                   (traits/determine-traits pact)))
        -deck-query (atom "")
        -shop-query (atom "")
        save-character
        #(-> {:name @-name
              :biography @-biography
              :level @-level
              :sanctified @-sanctified
              :exiled @-exiled
              :equipped []
              :inventory []
              :media []
              :wealth @-wealth}
             (db/save-character! id))
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
        reset-traits #(reset! -traits
                              (when-let [pact (seq (into @-exiled @-sanctified))]
                                (traits/determine-traits pact)))
        refresh-deck   (partial refresh-node "deck" list-own-deck)
        refresh-traits (partial refresh-node "traits" list-own-traits)
        refresh-stats  (partial refresh-node "stats" list-own-stats)]
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
    (add-watch -deck-query :query refresh-deck)
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
      [:div.content (ct/show-character character)]
      (character-not-found custom-characters character-ref))))

(defn new-character []
  (edit-character :on-save #(goto :show-character %)))

(defn template-character [custom-characters]
  (let [example-id (keyword (route-pattern :template-character))
        character (first (filter #(= example-id (:id %)) c/examples))]
    (if character
      (edit-character :character character
                      :on-save #(goto :show-character %))
      (character-not-found custom-characters example-id))))

(defn edit-custom-character [custom-characters]
  (let [character-ref (route-pattern :edit-character)
        character (get custom-characters character-ref)]
    (if character
      (edit-character :character character
                      :on-save #(goto :show-character %)
                      :id character-ref)
      (character-not-found custom-characters character-ref))))

(defn list-characters [custom-characters]
  [:div.content
   [:h1.title "Characters"]
   (ct/list-characters custom-characters :edit? false)])
