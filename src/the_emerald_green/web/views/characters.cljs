(ns the-emerald-green.web.views.characters
  (:require
   [the-emerald-green.characters :as c]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.web.templates.characters :as ct]
   [the-emerald-green.web.utils :refer [refresh-node]]))

(def sanctify-msg "Will you hold it sacred?")
(def nothing-sacred "Is nothing sacred to you?")
(def exile-msg "Do you have no need of it?")
(def nothing-exiled "Unburden yourself...")

(defn on-choice [maximum -choices confirm-msg]
  (when (<= (count @-choices) maximum)
    (fn [card]
      (when (js/confirm (str (:name card) "... " confirm-msg))
        (swap! -choices conj card)))))

(defn on-sanctify [level -sanctified]
  (on-choice (c/max-sanctified level) -sanctified sanctify-msg))

(defn on-exile [level -exiled]
  (on-choice (c/max-exiled level) -exiled exile-msg))

(defn list-chosen [empty-msg -chosen]
  (let [on-restore (partial swap! -chosen disj)]
    #(ct/list-chosen @-chosen
                     :on-restore on-restore
                     :empty-msg empty-msg)))

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
        list-own-deck
        #(ct/filter-deck
          (fn [card]
            (c/card-available? card
                               :sanctified @-sanctified
                               :exiled @-exiled
                               :query @-query))
          :on-exile (on-exile level -exiled)
          :on-sanctify (on-sanctify level -sanctified))
        list-sanctified (list-chosen nothing-sacred -sanctified)
        list-exiled (list-chosen nothing-exiled -exiled)
        list-own-traits #(ct/list-traits @-traits)
        list-own-stats #(ct/list-stats-from-traits level @-traits)
        reset-traits #(reset! -traits (traits/determine-traits
                                       (into (set (map :id @-exiled))
                                             (set (map :id @-sanctified)))))
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
    (add-watch -query :query refresh-deck)
    (ct/edit-character -name
                       -biography
                       -query
                       :deck (list-own-deck)
                       :traits (list-own-traits)
                       :exiled (list-exiled)
                       :sanctified (list-sanctified)
                       :new? (some? character)
                       :on-save on-save
                       :on-cancel on-cancel)))
