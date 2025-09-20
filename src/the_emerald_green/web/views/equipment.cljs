(ns the-emerald-green.web.views.equipment
  (:require
   [clojure.string :as string]
   [the-emerald-green.core :as core]
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.help :as help]
   [the-emerald-green.money :as money]
   [the-emerald-green.utils :refer [keyword->name name->keyword]]
   [the-emerald-green.web.db :as db]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.routing :refer [goto redirect route-pattern]]
   [the-emerald-green.web.utils :refer [atomify markdown-tip marshal-thing
                                        refresh-node]]))


(def prompt-name
  #(prompts/field "Name"
                  "Human-readable title of the thing."
                  prompts/text %))

(def prompt-description
  #(prompts/field "Description"
                  markdown-tip
                  prompts/textarea %))

(def prompt-content-pack
  #(prompts/field "Content Pack"
                  "What content pack, campaign, or setting is this associated with?"
                  prompts/text %))

(def prompt-enchantments
  #(prompts/field "Enchantments"
                  "Comma separated!"
                  prompts/autocomplete-many %
                  (keys equipment/id->enchantment)))

(def prompt-tags
  #(prompts/field "Tags"
                  "Comma separated!"
                  prompts/autocomplete-many %
                  equipment/all-tags))

(defn prompt-cost [-cost]
  (add-watch -cost :to-gold (fn [& _]
                              (refresh-node "cost-as-gold" #(money/wealth-to-gold @-cost))))
  [[:label.label "Cost"]
   [:p.help "The value of the thing, in copper pieces."]
   [:div.field.has-addons
    [:p.control
     [:a.button.is-static#cost-as-gold (money/wealth-to-gold @-cost)]]
    [:p.control {:style "width: 100%;"} (prompts/number -cost :props {:min 0})]]])

(defn make-thing [thing & {:keys [on-save on-cancel]}]
  (let [{:keys [-name -description -content-pack -enchantments -tags
                -cost -rarity]
         :as atoms}
        (atomify thing
                 :name ""
                 :description #(if % (string/replace % #"\n\s+" "\n") "")
                 :content-pack #(if % (keyword->name %) "")
                 :enchantments #(string/join ", " (map keyword->name (or % [])))
                 :tags #(string/join ", " (map keyword->name (or % [])))
                 :cost (fnil money/->cost 0)
                 :rarity ""
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
         [:div.block
          [:div.block
           (prompts/field "Inertia"
                          "Slows you down, complicates finesse."
                          prompts/number -inertia)]
          [:div.block
           [:p.subtitle "Resistances"]
           [:p.help "Amount of damage prevented from attacks that strike you."]
           (for [[element -value] elements
                 :let [element-kw (keyword (subs (name element) 1))]]
             [:div.block
              (prompts/field (keyword->name element-kw)
                             (help/get-help element-kw)
                             prompts/number -value)])]]))
     (when (= :tool (:type thing))
       (let [{:keys [-skill]}
             atoms]
         [:div.block
          (prompts/field "Skill"
                         "The skill relevant to using the tool."
                         prompts/choose-one -skill
                         core/ordered-skills)]))
     #_(when (= :consumable (:type thing))
         (let [{:keys [-effect]}
               atoms]
           [:p "TODO"]))
     [:div.block>div.buttons
      (when on-save
        [:button.button.is-primary.is-fullwidth
         {:onclick
          (fn [& _]
            (on-save (marshal-thing atoms
                                    :type (constantly (:type thing))
                                    :abstract (constantly false)
                                    :cost money/wealth-to-gold
                                    :content-pack name->keyword
                                    :enchantments
                                    (fn [raw-enchantments]
                                      (if-let [enchantments (seq (filter seq (string/split raw-enchantments #",\s*")))]
                                        (set (map name->keyword enchantments))
                                        #{}))
                                    :tags
                                    (fn [raw-tags]
                                      (if-let [tags (seq (filter seq (string/split raw-tags #",\s*")))]
                                        (set (map name->keyword tags))
                                        #{})))))}
         "Save"])
      (when on-cancel
        [:button.button.is-small.is-dark.is-fullwidth
         {:onclick on-cancel}
         "Cancel"])]]))

(defn save-equipment! [thing]
  (.then (db/save-equipment! thing)
         #(goto :equipment-guide)))

(defn from-template [custom-stuff]
  (let [thing-id (route-pattern :template-stuff)]
    (if-let [thing (get custom-stuff thing-id
                        (equipment/id->equipment (keyword thing-id)))]
      [[:h1 "Create from Template"]
       (make-thing thing :on-save save-equipment!)]
      #(redirect :404))))

(defn design-equipment []
  (let [-type (atom "")]
    (add-watch -type :design
               (fn [& _]
                 (refresh-node "new-equipment" #(make-thing {:type @-type} :on-save save-equipment!))))
    [[:h1 "Design Equipment"]
     [:div.block
      (prompts/field "Equipment Type"
                     "What is this thing?"
                     prompts/choose-one -type
                     equipment/stuff-types)]
     [:div.block#new-equipment]]))

(defn edit-equipment [custom-stuff]
  (let [thing-id (route-pattern :edit-stuff)]
    (if-let [thing (get custom-stuff thing-id
                        (equipment/id->equipment (keyword thing-id)))]
      [[:h1 "Edit Equipment"]
       (make-thing thing :on-save save-equipment!)]
      #(redirect :404))))
