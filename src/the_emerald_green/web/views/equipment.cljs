(ns the-emerald-green.web.views.equipment 
  (:require
   [clojure.string :as string]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.utils :refer [keyword->name name->keyword]]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.routing :refer [route-pattern]]))

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
    [(prompts/field "Name"
                    "Human-readable title of the thing."
                    prompts/text -name)
     (prompts/field "Description"
                    "Use Markdown!"
                    prompts/textarea -description)
     (prompts/field "Content Pack"
                    "What content pack, campaign, or setting is this associated with?"
                    prompts/text -content-pack)
    ;;  TODO choose-from: heft, skill, element, range, rarity
    ;;  TODO wealth (cost as number, display wealth beside it)
    ;;  TODO autocomplete from known content IDs
     (prompts/field "Enchantments"
                    "Comma separated!"
                    prompts/dropdown -enchantments
                    (fn [query] [])
                    (fn [selected] nil))
     (prompts/field "Tags"
                    "Comma separated!"
                    prompts/dropdown -tags
                    (fn [query] [])
                    (fn [selected] nil))
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
