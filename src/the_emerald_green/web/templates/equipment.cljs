(ns the-emerald-green.web.templates.equipment 
  (:require
   ["marked" :as marked]
   [clojure.string :as string]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.web.alchemy :refer [profane]]
   [the-emerald-green.web.utils :refer [pprint]]))

(def lolraw #(vec [:pre>code (with-out-str (pprint %))]))

(defn describe-weapon [{weapon-name :name
                        :keys [description tags]
                        :as weapon}
                       & {open? :open
                          :or {open? ""}}]
  [:details
   {:open open?}
   [:summary
    [:span [:em weapon-name] (str " [" (string/join (map name tags)) "]")]]
   [:div
    (profane "p" (marked/parse description))
    [:p "Tags: " (string/join (map name (:tags weapon)) ", ")]
    (lolraw (select-keys weapon [:tags :element :name :enchantments :cost :rarity]))]])
(defn describe-armor [armor])
(defn describe-tool [tool])
(defn describe-consumable [consumable])
(defn describe-item [item])

(def type->describe
  {:weapon describe-weapon})

(defn equipment-guide [type->stuff]
  (let [type->stuff (or type->stuff equipment/type->stuff)]
    [:div.content
     [:h1.title "Equipment Guide"]
     (for [[stuff-type title] [[:weapon "Weapons"]]
           :let [stuff (type->stuff stuff-type)
                 describe-thing (type->describe stuff-type)]]
       [:div
        [:h2.subtitle [:strong title]]
        (map describe-thing stuff)])]))
