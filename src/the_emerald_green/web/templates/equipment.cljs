(ns the-emerald-green.web.templates.equipment
  (:require
   ["marked" :as marked]
   [clojure.string :as string]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.utils :refer [keyword->name]]
   [the-emerald-green.web.alchemy :refer [profane snag]]
   [the-emerald-green.web.routing :refer [route->href]]
   [the-emerald-green.web.utils :refer [lolraw scroll-to-id]]
   [the-emerald-green.help :as help]))

(defn thing-hash-id [{stuff-type :type id :id id_ :_id}]
  (str (name stuff-type) "_" (name (or id id_))))

(defn craftbench [{id :id _id :_id}]
  (let [thing-id (if id (name id) _id)]
    [:div.buttons
     [:a.button.is-fullwidth (route->href :template-stuff thing-id) "Use as Template"]
     (when _id
       [:a.button.is-fullwidth.is-light (route->href :edit-stuff _id) "Edit"])]))

(defn describe-thing [notables
                      {thing-name :name
                       :keys [description tags]
                       :as thing}]
  [(str "div.box#" (thing-hash-id thing))
   [:h5 [:em thing-name] (when (seq tags) (str " [" (string/join (map name tags)) "]"))]
   [:div
    (profane "p" (marked/parse description))
    [:ul
     (for [key notables
           :let [raw-value (get thing key)
                 key-name (keyword->name key)
                 value
                 (cond
                   (map? raw-value)
                   (when-let [seq-map (seq raw-value)]
                     (string/join " ; " (map #(str (keyword->name (first %)) " => " (second %)) seq-map)))
                   (keyword? raw-value)    (keyword->name raw-value)
                   (sequential? raw-value) (seq raw-value)
                   (set? raw-value)
                   (interpose
                    ", "
                    (for [sub-value raw-value
                          :let [value-name (keyword->name sub-value)]]
                      [:span (help/tag->title sub-value) value-name]))
                   :else raw-value)]
           :when (not
                  (or (nil? value)
                      (false? value)
                      (zero? value)
                      (when (sequential? value) (nil? (seq value)))))]
       [:li
        [:span (help/tag->title key) key-name]
        ": "
        [:span (when (keyword? raw-value) (help/tag->title raw-value)) value]])
     [:li
      [:details
       [:summary "Definition"]
       (lolraw thing)]]]
    (craftbench thing)]])

(def base-props [:cost :rarity :content-pack :enchantments :tags])
(def describe-weapon (partial describe-thing (concat [:heft :range :element] base-props)))
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

(def section-titles
  [[:weapon "Weapons"]
   [:armor "Armor"]
   [:tool "Tools"]
   [:consumable "Consumables"]
   [:item "Items"]])

(defn equipment-guide-nav [type->stuff]
  [[:p>strong "Table of Contents"]
   (for [[stuff-type title] section-titles
         :let [stuff (sort-by :name (type->stuff stuff-type))
               {real-stuff true
                abstract-stuff false}
               (group-by (comp false? :abstract) stuff)
               on-click
               (fn [id abstract?]
                 (when abstract?
                   (.setAttribute (snag (str "abstract-" stuff-type)) "open" true))
                 (scroll-to-id id))
               stuff-toc
               #(vec
                 [:ul
                  (for [{thing-name :name
                         abstract? :abstract
                         :as thing} %
                        :let [id (thing-hash-id thing)]]
                    [:li>a {:onclick (partial on-click id abstract?)} thing-name])])]]
     [:div.block
      [:p>em title]
      (stuff-toc real-stuff)
      (when (seq abstract-stuff)
        [:details
         [:summary "Abstract Base Types"]
         (stuff-toc abstract-stuff)])
      [:br]])])

(defn equipment-guide-tables [type->stuff]
  (for [[stuff-type title] section-titles
        :let [stuff (sort-by :name (type->stuff stuff-type))
              describe-thing (type->describe stuff-type)
              {real-stuff true
               abstract-stuff false}
              (group-by (comp false? :abstract) stuff)]
        :when (and describe-thing
                   (seq stuff))]
    [:div.block
     [:h3.subtitle title]
     (when (seq real-stuff)
       (map describe-thing real-stuff))
     (when (seq abstract-stuff)
       [(str "details#abstract-" stuff-type)
        [:summary "Abstract Base Types"]
        (map describe-thing abstract-stuff)])]))

(defn equipment-guide [custom-stuff]
  (let [type->custom-stuff (group-by :type (vals custom-stuff))
        type->stuff (merge-with concat equipment/type->stuff type->custom-stuff)]
    [:div.block
     [:h1.title "Equipment Guide"]
     (equipment-guide-nav type->stuff)
     [:hr]
     (equipment-guide-tables type->stuff)]))
