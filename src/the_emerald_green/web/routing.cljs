(ns the-emerald-green.web.routing
  (:require [the-emerald-green.web.alchemy :refer [snag]]))

(def default-route :introduction)

(def route->hash
  {:introduction    "#/introduction"
   :player-guide    "#/guides/player"
   :card-guide      "#/guides/cards"
   :trait-guide     "#/guides/traits"
   :equipment-guide "#/guides/equipment"
   :setting-guide   "#/guides/setting"
   :gm-guide        "#/guides/gm"
   :new-character   "#/characters/new"
   :characters      "#/characters"
   :campaigns       "#/campaigns"
   :search          "#/search"})

(defn route->href [route]
  {:href (route->hash route)})

(defn goto-str [s]
  (set! js/window.location s))

(defn goto-search [query]
  (goto-str (str (route->hash :search) "/" query)))

(defn goto [route] (goto-str (get route->hash route)))

(defn find-view [route->view url-hash]
  (->> (keys route->view)
       (filter #(re-find (re-pattern %) url-hash))
       sort
       last
       route->view))

(defn handle-refresh [route->view main-id]
  (if-let [view (find-view route->view js/document.location.hash)]
    (view (snag main-id))
    (goto default-route)))
