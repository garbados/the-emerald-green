(ns the-emerald-green.web.templates.tools
  (:require
   [clojure.string :as string]
   #_[the-emerald-green.traits :as traits]
   [the-emerald-green.deck :as deck]))

(defn new-character []
  [:div.content
   [:h1 "New Character"]
   [:div.box
    {:style "overflow: scroll; height: 300px;"}
    [:table.table.is-hoverable
     [:thead
      [:tr
       [:th "Name"]
       [:th "Exile!"]
       [:th "Sanctify!"]]]
     (cons :tbody
           (for [card deck/base-deck]
             [:tr
              [:td (:name card)]
              [:td [:a "Exile!"]]
              [:td [:a "Sanctify!"]]]))]]
   [:div
    (for [card deck/base-deck]
      [:div.box
       [:h1 (:name card)]
       [:p "Rank: " (:rank card) " (+" (deck/rank->mod (:rank card)) ")"]
       [:p "Tags: " (string/join ", " (map deck/arcana-keyword->name (:tags card)))]])]])

(defn characters []
  [:div.content
   [:h1 "Characters"]
   [:p "TODO"]])

(defn campaigns []
  [:div.content
   [:h1 "Campaigns"]
   [:p "TODO"]])

(defn search [query]
  [:div.content
   [:h1 (str "Search: " query)]
   [:p "TODO"]])
