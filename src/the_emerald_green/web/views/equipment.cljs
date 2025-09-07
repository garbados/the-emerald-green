(ns the-emerald-green.web.views.equipment 
  (:require
   [clojure.string :as string]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.utils :refer [keyword->name name->keyword]]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.routing :refer [route-pattern]]))

(defn prompt-field [label prompt -atom]
  [:div.field
   [:label.label label]
   [:div.control (prompt -atom)]])

(defn make-weapon
  [weapon
   & {:keys [on-save on-cancel]}]
  (let [-name (atom (:name weapon ""))
        -description (atom (:description weapon ""))
        -content-pack (atom (:content-pack weapon ""))
        -skill (atom (:skill weapon :melee))
        -heft (atom (:heft weapon :light))
        -element (atom (:element weapon :physical))
        -range (atom (:range weapon :close))
        -enchantments (atom (string/join ", " (map :name (:enchantments weapon []))))
        -cost (atom (:cost weapon 0))
        -rarity (atom (:rarity weapon :common))
        -tags (atom (string/join ", " (map keyword->name (:tags weapon []))))
        marshal-weapon
        #(hash-map :type :weapon
                   :name @-name
                   :id (str (random-uuid))
                   :description @-description
                   :skill @-skill
                   :heft @-heft
                   :element @-element
                   :range @-range
                   :cost @-cost
                   :rarity @-rarity
                   :enchantments
                   (->> (string/split @-enchantments #"\s*,\s*")
                        (map name->keyword)
                        set)
                   :tags
                   (->> (string/split @-tags #"\s*,\s*")
                        (map name->keyword)
                        set))]
    [(prompt-field "Name" prompts/text -name)
     (prompt-field "Description" prompts/textarea -description)
     (prompt-field "Content Pack" prompts/text -content-pack)
    ;;  TODO choose-from: heft, skill, element, range, rarity
    ;;  TODO wealth (cost as number, display wealth beside it)
    ;;  TODO autocomplete from known content IDs
     [:div.field
      [:label.label "Enchantments"]
      [:div.control (prompts/text -enchantments)]
      [:p.help "Comma separated!"]]
     [:div.field
      [:label.label "Tags"]
      [:div.control (prompts/text -tags)]
      [:p.help "Comma separated!"]]
     [:div.buttons
      (when on-save
        [:button.button
         {:onclick #(on-save (marshal-weapon))}
         "Save"])
      (when on-cancel
        [:button.button
         {:onclick on-cancel}
         "Cancel"])]]))

(def type->maker
  {:weapon make-weapon})

(defn from-template []
  (let [thing-id (route-pattern :template-stuff)
        {thing-type :type :as thing} (equipment/id->equipment (keyword thing-id))]
    [:div.content
     [:h1 "Create from Template"]
     (when-let [thing-maker (type->maker thing-type)]
       (thing-maker thing))]))

(defn design-equipment []
  [:h1 "TODO"])

(defn edit-equipment []
  [:h1 "TODO"])
