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
  (str (name stuff-type) "_" (if id (name id) (or id_ (str (random-uuid))))))

(defn craftbench [{id :id _id :_id}]
  (let [thing-id (if id (name id) _id)]
    [:div.buttons
     [:a.button.is-fullwidth (route->href :template-stuff thing-id) "Use as Template"]
     (when _id
       [:a.button.is-fullwidth.is-light (route->href :edit-stuff _id) "Edit"])]))

(defn describe-thing [{thing-name :name
                       thing-type :type
                       :keys [description tags]
                       :as thing}
                      & {:keys [craftable?]
                         :or {craftable? true}}]
  [(str "div.box#" (thing-hash-id thing))
   [:h5 [:em thing-name] (when (seq tags) (str " [" (string/join (map name tags)) "]"))]
   [:div
    (profane "blockquote" (marked/parse description))
    [:ul
     (for [key (equipment/type->props thing-type)
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
    (when craftable?
      (craftbench thing))]])

(defn summarize-thing [{thing-type :type :as thing}]
  (string/join
   "\n"
   (concat
    [(:name thing)
     (str "> " (string/replace (:description thing) #"\n\s+" "\n> "))]
    (for [prop (equipment/type->props thing-type)
          :let [value (get thing prop)
                value
                (cond
                  (map? value)
                  (string/join ", " (map #(str %1 ": " %2)
                                         (map keyword->name (keys value))
                                         (vals value)))
                  (keyword? value)
                  (keyword->name value)
                  (some true? ((juxt sequential? set?) value))
                  (string/join ", " (map keyword->name value))
                  :else
                  value)]
          :when (if (string? value)
                  (seq value)
                  value)]
      (str (keyword->name prop) ": " value)))))



(defn equipment-guide-nav [type->stuff]
  [[:p>strong "Table of Contents"]
   (for [stuff-type equipment/stuff-types
         :let [title (equipment/type->title stuff-type)
               stuff (sort-by :name (type->stuff stuff-type))
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
         (stuff-toc abstract-stuff)])])])

(defn equipment-guide-tables [type->stuff]
  (for [stuff-type equipment/stuff-types
        :let [title (equipment/type->title stuff-type)
              stuff (sort-by :name (type->stuff stuff-type))
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
  (let [type->stuff (equipment/merge-custom-stuff custom-stuff)]
    [:div.block
     [:h1.title "Equipment Guide"]
     (equipment-guide-nav type->stuff)
     [:hr]
     (equipment-guide-tables type->stuff)]))
