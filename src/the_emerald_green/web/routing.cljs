(ns the-emerald-green.web.routing
  (:require [the-emerald-green.web.alchemy :refer [snag]]))

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

(def default-route :introduction) ; put a 404 here someday?

(defn route->href [route]
  {:href (route->hash route)})

(defn goto-str [s]
  (set! js/window.location s))

(defn goto-search [query]
  (goto-str (str (route->hash :search) "/" query)))

(defn goto
  ([route]
   (goto route default-route))
  ([route default-route]
   (goto-str (get route->hash route default-route))))

(defn handle-route [routes node hash default-route]
  ;; using hash fragments as regex, get the most specific match
  (let [matched (last (sort (filter #(re-find (re-pattern %) hash) (keys routes))))]
    (if-let [handler (get routes matched)]
      (handler node hash)
      (goto default-route))))

(defn handle-refresh [routes main-id default-route]
  (handle-route routes (snag main-id) js/document.location.hash default-route))
