(ns the-emerald-green.web.views.equipment
  (:require
   [clojure.string :as string]
   [the-emerald-green.core :as core]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.help :as help]
   [the-emerald-green.money :as money]
   [the-emerald-green.utils :refer [keyword->name]]
   [the-emerald-green.web.db :as db]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.routing :refer [goto redirect route-pattern]]
   [the-emerald-green.web.utils :refer [refresh-node]]))

(def immutable #{:abstract :type :id :extends})

(defn atomify [thing & {:as defaults}]
  (into {}
        (for [prop (concat (keys thing) (keys defaults))
              :let [value (get thing prop)
                    default (get defaults prop)
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

(def prompt-name
  #(prompts/field "Name"
                  "Human-readable title of the thing."
                  prompts/text %))

(def prompt-description
  #(prompts/field "Description"
                  help/markdown-tip
                  prompts/textarea %))

(def prompt-content-pack
  #(prompts/field "Content Pack"
                  "What content pack, campaign, or setting is this associated with?"
                  prompts/text %))

(def prompt-enchantments
  #(prompts/field "Enchantments"
                  "Comma separated!"
                  prompts/autocomplete-one %
                  (keys equipment/id->enchantment)))

(def prompt-tags
  #(prompts/field "Tags"
                  "Comma separated!"
                  prompts/autocomplete-one %
                  equipment/all-tags))

(defn prompt-cost [-cost]
  (add-watch -cost :to-gold (fn [& _]
                              (refresh-node "cost-as-gold" #(money/wealth-to-gold @-cost))))
  [[:label.label "Cost"]
   [:div.field.has-addons
    [:p.control
     [:a.button.is-static#cost-as-gold (money/wealth-to-gold @-cost)]]
    [:p.control {:style "width: 100%;"} (prompts/number -cost)]]
   [:p.help "The value of the thing, in copper pieces."]])

(defn make-thing [thing & {:keys [on-save on-cancel]}]
  (let [{:keys [-name -description -content-pack -tags -enchantments
                -cost -rarity]
         :as atoms
         :or {-tags (atom "")
              -enchantments (atom "")}}
        (atomify thing
                 :name ""
                 :description ""
                 :content-pack ""
                 :enchantments #(string/join ", " (map keyword->name (:enchantments % [])))
                 :tags #(string/join ", " (map keyword->name (:tags % [])))
                 :cost money/->cost
                 :skill ""
                 :heft ""
                 :element ""
                 :range ""
                 :resistances {}
                 :inertia 0
                 :effect {})]
    [[:div.block
      (for [prompt [(prompt-name -name)
                    (prompt-description -description)
                    (prompt-content-pack -content-pack)
                    (prompt-enchantments -enchantments)
                    (prompt-tags -tags)
                    (prompt-cost -cost)
                    (prompts/field "Rarity"
                                   "How common, or easy to obtain, is the thing?"
                                   prompts/choose-one -rarity
                                   equipment/rarities)]]
        [:div.block prompt])]
     (when (= :weapon (:type thing))
       (let [{:keys [-skill -heft -element -range]}
             atoms]
         [:div.block
          (for [prompt [(prompts/field "Skill"
                                       "The skill relevant to using the weapon."
                                       prompts/choose-one -skill
                                       equipment/weapon-skills)
                        (prompts/field "Heft"
                                       "How hard the weapon is to wield, and how hard it hits."
                                       prompts/choose-one -heft
                                       equipment/weapon-hefts)
                        (prompts/field "Element"
                                       "The element associated with the weapon. Attacks inflict this type of damage."
                                       prompts/choose-one -element
                                       equipment/elements)
                        (prompts/field "Range"
                                       "The distance at which this weapon is most effective."
                                       prompts/choose-one -range
                                       equipment/weapon-ranges)]]
            [:div.block prompt])]))
     (when (= :armor (:type thing))
       (let [{:keys [-resistances -inertia]}
             atoms
             {:as elements}
             (atomify
              (merge
               (zipmap equipment/elements (repeat 0))
               @-resistances))]
         (doseq [[element -value] elements]
           (add-watch -value element #(swap! -resistances assoc element @-value)))
         [[:div.block
           (prompts/field "Inertia"
                          "Slows you down, complicates finesse."
                          prompts/number -inertia)]
          [:div.block
           [:p>strong "Resistances"]
           [:p.help "Amount of damage prevented from attacks that strike you."]
           (for [[element -value] elements]
             (prompts/field (keyword->name element)
                            (help/get-help element)
                            prompts/number -value))]]))
     (when (= :tool (:type thing))
       (let [{:keys [-skill]}
             atoms]
         [:div.block
          (prompts/field "Skill"
                         "The skill relevant to using the tool."
                         prompts/choose-one -skill
                         core/ordered-skills)]))
     (when (= :consumable (:type thing))
       (let [{:keys [-effect]}
             atoms]
         [:p "TODO"]))
     [:div.block>div.buttons
      (when on-save
        [:button.button.is-primary.is-fullwidth
         {:onclick #(on-save (marshal-thing atoms))}
         "Save"])
      (when on-cancel
        [:button.button.is-small.is-dark.is-fullwidth
         {:onclick on-cancel}
         "Cancel"])]]))

(defn from-template [custom-stuff]
  (let [thing-id (route-pattern :template-stuff)]
    (if-let [thing (get custom-stuff thing-id
                        (equipment/id->equipment (keyword thing-id)))]
      [[:h1 "Create from Template"]
       (make-thing thing :on-save #(do (db/save-equipment! %)
                                       (goto :equipment-guide)))]
      #(redirect :404))))

(defn design-equipment [custom-stuff]
  [:h1 "TODO"])

(defn edit-equipment [custom-stuff]
  [:h1 "TODO"])
