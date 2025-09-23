(ns the-emerald-green.web.templates.layout
  (:require
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.routing :refer [goto-search route->href default-route]]))

(def navbar
  [:nav.navbar
   {:role "navigation" :aria-label "main navigation"}
   [:div.navbar-brand
    [:a.navbar-item
     (route->href default-route)
     [:h1.title "🍃 The Emerald Green"]]]
   [:div.navbar-menu
    [:div.navbar-end
     [:div.navbar-item
      [:div.field
       [:div.control.has-icons-right
        (prompts/text (atom "")
                      :on-submit goto-search
                      :placeholder "🔍 Search content")]]]
     [:div.navbar-item
      [:a.button.is-info.is-light
       {:href "https://github.com/garbados/the-emerald-green"
        :target "_blank"}
       "Source 👩‍💻"]]]]])

(def menu-bar
  [:aside.menu.box
   [:ul.menu-list
    [:li [:a (route->href :introduction) "📖 Introduction"]]]
   [:p.menu-label "Guides"]
   [:ul.menu-list
    [:li [:a (route->href :player-guide) "📕 Player Guide"]]
    [:li [:a (route->href :card-guide) "🃏 Card Guide"]]
    [:li [:a (route->href :trait-guide) "📗 Trait Guide"]]
    [:li [:a (route->href :equipment-guide) "📘 Equipment Guide"]]
    [:li [:a (route->href :setting-guide) "📙 Setting Guide"]]
    [:li [:a (route->href :gm-guide) "📓 GM Guide"]]]
   [:p.menu-label "Game Tools"]
   [:ul.menu-list
    [:li [:a (route->href :new-character) "🐣 New Character"]]
    [:li [:a (route->href :characters) "🐺 Characters"]]
    [:li [:a (route->href :invent-stuff) "🛠️ New Equipment"]]
    [:li [:a (route->href :campaigns) "🧝 Campaigns"]]]])

(def container
  [:div
   navbar
   [:section.section
    [:div.container-fluid
     [:div.columns.is-desktop
      [:div.column.is-narrow menu-bar]
      [:div.column
       [:div.box
        [:div.content#main
         [:h1.title "Loading..."]]]]]]]])
