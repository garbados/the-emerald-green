(ns the-emerald-green.web.templates.equipment
  (:require
   ["marked" :as marked]
   [clojure.string :as string]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.utils :refer [keyword->name]]
   [the-emerald-green.web.alchemy :refer [profane]]
   [the-emerald-green.web.routing :refer [route->href]]
   [the-emerald-green.web.utils :refer [lolraw]]))

(defn craftbench [{_id :_id abstract? :abstract :as thing}]
  [:div.buttons
   (when abstract?
     [:a.button.is-fullwidth (route->href :template-stuff (-> thing :id name)) "Use as Template"])
   (when _id
     [:a.button.is-fullwidth (route->href :edit-stuff _id) "Edit"])])

(defn describe-thing [notables
                      {thing-name :name
                       :keys [description tags]
                       :as thing}]
  [:div.box
   [:h5 [:em thing-name] (when (seq tags) (str " [" (string/join (map name tags)) "]"))]
   [:div
    (profane "p" (marked/parse description))
    [:ul
     (for [key notables
           :let [raw-value (get thing key)
                 key-name (keyword->name key)
                 value
                 (cond
                   (map? raw-value) (lolraw raw-value)
                   (keyword? raw-value) (keyword->name raw-value)
                   (sequential? raw-value) (seq raw-value)
                   :else raw-value)]
           :when value]
       [:li key-name ": " value])
     [:li
      [:details
       [:summary "Definition"]
       (lolraw thing)]]]
    (craftbench thing)]])

(def base-props [:cost :rarity :content-pack])
(def describe-weapon (partial describe-thing (concat [:heft :range :element :enchantments] base-props)))
(def describe-armor (partial describe-thing (concat [:resistances :inertia] base-props)))
(def describe-tool (partial describe-thing (concat [:skill] base-props)))
(def describe-consumable (partial describe-thing (concat [:effect] base-props)))
(def describe-item (partial describe-thing base-props))

(def type->describe
  {:weapon describe-weapon
   :armor describe-armor
   :tool describe-tool
   :consumable describe-consumable
   :item describe-item})

(defn equipment-guide [custom-stuff]
  (let [type->custom-stuff (group-by :type (vals custom-stuff))
        type->stuff (merge-with concat equipment/type->stuff type->custom-stuff)]
    [:div.content
     [:h1.title "Equipment Guide"]
     (for [[stuff-type title] [[:weapon "Weapons"]
                               [:armor "Armor"]
                               [:tool "Tools"]
                               [:consumable "Consumables"]
                               [:item "Items"]]
           :let [stuff (type->stuff stuff-type)
                 describe-thing (type->describe stuff-type)
                 {real-stuff true
                  abstract-stuff false}
                 (group-by (comp not some? :abstract) stuff)]
           :when (and describe-thing
                      (seq stuff))]
       [:div
        [:h3 title]
        (when (seq abstract-stuff)
          [:details
           [:summary "Abstract Base Types"]
           [:div.box
            (map describe-thing abstract-stuff)]])
        (when (seq real-stuff)
          [[:hr]
           (map describe-thing real-stuff)])])]))
