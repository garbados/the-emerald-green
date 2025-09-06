(ns the-emerald-green.web.templates.tools
  (:require
   [clojure.string :as string]
   [the-emerald-green.web.routing :refer [route->hash]]))

(defn campaigns []
  [:div.content
   [:h1 "Campaigns"]
   [:p "TODO"]])

(def search-re (re-pattern (str (route->hash :search) "/")))
(defn search []
  (let [query (string/replace-first js/document.location.hash search-re "")]
    [:div.content
     [:h1 (str "Search: " query)]
     [:p "TODO"]]))
