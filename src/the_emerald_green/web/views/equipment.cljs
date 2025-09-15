(ns the-emerald-green.web.views.equipment
  (:require
   [clojure.string :as string]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.utils :refer [keyword->name]]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.routing :refer [route-pattern]]
   [the-emerald-green.help :as help]))

(def immutable #{:abstract :type :id :extends})

(defn atomify [thing & {:as defaults}]
  (into {}
        (for [[prop value] thing
              :let [default (get defaults prop)
                    transform? (ifn? default)]
              :when (not (contains? immutable prop))]
          [(keyword (str "-" (name prop)))
           (atom (if transform? (default value) (or value default)))])))

(defn marshal-thing [atoms & {:as transforms}]
  (into {}
        (for [[prop* -value] atoms
              :let [prop (keyword (subs (name prop*) 1))
                    value
                    (if-let [transform (get transforms prop)]
                      (transform @-value)
                      @-value)]]
          [prop value])))

(def describe-name
  #(prompts/field "Name"
                  "Human-readable title of the thing."
                  prompts/text %))

(def describe-description
  #(prompts/field "Description"
                  help/markdown-tip
                  prompts/textarea %))

(def describe-content-pack
  #(prompts/field "Content Pack"
                  "What content pack, campaign, or setting is this associated with?"
                  prompts/text %))

;;  TODO autocomplete from known content IDs
(def describe-enchantments
  #(prompts/field "Enchantments"
                  "Comma separated!"
                  prompts/dropdown %
                  (fn [query] 'todo)
                  (fn [selected] 'todo)))

(def describe-tags
  #(prompts/field "Tags"
                  "Comma separated!"
                  prompts/dropdown %
                  (fn [query] 'todo)
                  (fn [selected] 'todo)))

(defn make-thing [thing & {:keys [on-save on-cancel]}]
  (let [{:keys [-name -description -content-pack -tags -enchantments]
         :as atoms
         :or {-tags (atom "")
              -enchantments (atom "")}}
        (atomify thing
                 :name ""
                 :description ""
                 :content-pack ""
                 :enchantments #(string/join ", " (map keyword->name (:enchantments % [])))
                 :tags #(string/join ", " (map keyword->name (:tags % []))))]
    [(describe-name -name)
     (describe-description -description)
     (describe-content-pack -content-pack)
     (describe-enchantments -enchantments)
     (describe-tags -tags)
    ;; TODO ::money/cost
    ;; TODO ::rarity
     (when (= :weapon (:type thing))
       (let [{:keys [-skill -heft -element -range]}
             atoms]
         [[:p "Skill: todo"]
          (prompts/field "Heft"
                         "Light, medium, heavy."
                         prompts/choose-one -heft
                         equipment/hefts)
          [:p "Element: todo"]
          [:p "Range: todo"]]))
     (when (= :armor (:type thing))
       (let [{:keys [-resistances -inertia]}
             atoms]
         [:p "TODO"]))
     (when (= :tool (:type thing))
       (let [{:keys [-skill]}
             atoms]
         [:p "TODO"]))
     (when (= :consumable (:type thing))
       (let [{:keys [-effect]}
             atoms]
         [:p "TODO"]))
     [:div.buttons
      (when on-save
        [:button.button
         {:onclick #(on-save (marshal-thing atoms))}
         "Save"])
      (when on-cancel
        [:button.button
         {:onclick on-cancel}
         "Cancel"])]]))

(defn from-template [custom-stuff]
  (let [thing-id (route-pattern :template-stuff)]
    [[:h1 "Create from Template"]
     (make-thing
      (get custom-stuff thing-id
           (equipment/id->equipment (keyword thing-id))))]))

(defn design-equipment [custom-stuff]
  [:h1 "TODO"])

(defn edit-equipment [custom-stuff]
  [:h1 "TODO"])
