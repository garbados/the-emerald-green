(ns the-emerald-green.web.templates.equipment
  (:require
   ["marked" :as marked]
   [clojure.string :as string]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.web.alchemy :refer [profane]]
   [the-emerald-green.web.routing :refer [route->href]]
   [the-emerald-green.web.utils :refer [lolraw]]))

(defn craftbench [{_id :_id abstract? :abstract :as thing}]
  [:div.buttons
   (when abstract?
     [:a.button.is-fullwidth (route->href :template-stuff (-> thing :id name)) "Use as Template"])
   (when _id
     [:a.button.is-fullwidth (route->href :edit-stuff _id) "Edit"])])

(defn describe-weapon [{weapon-name :name
                        :keys [description tags]
                        :as weapon}]
  [:div.box
   [:p [:em weapon-name] (when (seq tags) (str " [" (string/join (map name tags)) "]"))]
   [:div
    (profane "p" (marked/parse description))
    [:ul
     (for [key [:heft :range :element :cost :rarity]
           :let [value (get weapon key)]]
       [:li (string/capitalize (name key)) ": " value])
     [:li
      [:details
       [:summary "Definition"]
       (lolraw weapon)]]]
    (craftbench weapon)]])

(defn describe-armor [armor])
(defn describe-tool [tool])
(defn describe-consumable [consumable])
(defn describe-item [item])

(def type->describe
  {:weapon describe-weapon
   :armor describe-armor
   :tool describe-tool
   :consumable describe-consumable})

(defn equipment-guide [type->stuff]
  (let [type->stuff (or type->stuff equipment/type->stuff)]
    [:div.content
     [:h1.title "Equipment Guide"]
     (for [[stuff-type title] [[:weapon "Weapons"]
                               [:armor "Armor"]
                               [:tool "Tools"]
                               [:consumables "Consumables"]]
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
