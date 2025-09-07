(ns the-emerald-green.web.views.equipment 
  (:require
   [the-emerald-green.equipment :as equipment]
   [the-emerald-green.web.routing :refer [route-pattern]]))

(defn make-weapon [weapon & {:keys [on-save on-cancel]}]
  ())

(defn from-template []
  (let [thing-id (route-pattern :template-stuff)
        thing (equipment/id->equipment (keyword thing-id))]
    (println thing)
    [:h1 (name thing-id)]))

(defn design-equipment []
  [:h1 "TODO"])

(defn edit-equipment []
  [:h1 "TODO"])
