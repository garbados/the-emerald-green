(ns the-emerald-green.web.app
  (:require
   ["html-alchemist" :refer [snag profane] :as alchemy]
   ["marked" :as marked]
   [clojure.string :refer [ends-with?]]
   [shadow.cljs.modern :refer (defclass)]
   [the-emerald-green.utils :refer-macros [inline-slurp]]))

;; PREAMBLE

(defn alchemize
  "Clojure-friendly wrapper around Alchemist's alchemize function."
  [expr]
  (alchemy/alchemize (clj->js expr)))

;; ROUTING

(def route->hash
  {:landing       "#/"
   :player-guide  "#/guides/player"
   :new-character "#/characters/new"
   :characters    "#/characters"
   :campaigns     "#/campaigns"
   :search        "#/search"})

(defn route->href [route]
  {:href (route->hash route)})

(defn goto-str [s]
  (set! js/window.location s))

(defn goto-search [query]
  (goto-str (str (route->hash :search) "/" query)))

(defn goto
  ([route]
   (goto route "#/"))
  ([route default-route]
   (goto-str (get route->hash route default-route))))

(defn handle-route [routes node hash default-route]
  (let [matched (filter #(re-find (re-pattern %) hash) (keys routes))]
    (if-let [handler (get routes (first matched))]
      (handler node hash)
      (goto default-route))))

(defn handle-refresh [routes main-id default-route]
  (handle-route routes (snag main-id) js/document.location.hash default-route))

;; TEMPLATES

(defn prompt-text [-value & {:keys [on-submit placeholder]}]
  (let [oninput
        (fn [event]
          (.preventDefault event)
          (reset! -value (-> event .-target .-value)))
        onkeydown
        (fn [event]
          (when (= 13 (.-which event))
            (on-submit @-value)))]
    [:input.input
     (cond->
      {:type "text"
       :value @-value
       :oninput oninput}
       placeholder (assoc :placeholder placeholder)
       on-submit (assoc :onkeydown onkeydown))]))

(defn prompt-textarea [-value]
  [:textarea.textarea
   {:oninput #(reset! -value (-> % .-target .-value))
    :rows 10}
   @-value])

(defn navbar []
  [:div.box
   [:div.level
    [:div.level-left
     [:div.level-item
      [:h1.title "🍃 The Emerald Green"]]]
    [:div.level-right
     [:div.level-item
      [:div.field
       [:div.control.has-icons-right
        (prompt-text (atom "")
                     :on-submit goto-search
                     :placeholder "🔍 Search traits")]]]
     [:div.level-item
      [:a.button.is-info.is-light
       {:href "https://github.com/garbados/the-emerald-green"
        :target "_blank"}
       "Source 👩‍💻"]]]]])

(defn menu-bar []
  [:aside.menu
   [:ul.menu-list
    [:li [:a (route->href :landing) "📖 Introduction"]]]
   [:p.menu-label "The Guides"]
   [:ul.menu-list
    [:li [:a (route->href :player-guide) "📕 Player Guide"]]
    [:li [:a (route->href :trait-guide) "📗 Trait Guide"]]
    #_[:li [:a {:href (route->hash :setting-guide)} "📙 Setting Guide"]]
    #_[:li [:a {:href (route->hash :gm-guide)} "📘 GM Guide"]]]
   [:p.menu-label "Game Tools"]
   [:ul.menu-list
    [:li [:a (route->href :new-character) "🐣 New Character"]]
    [:li [:a (route->href :characters) "🐺 Characters"]]
    [:li [:a (route->href :campaigns) "🧝 Campaigns"]]]])

(defn container []
  [:div
   (navbar)
   [:div.block
    [:div.container
     [:div.columns
      [:div.column.is-narrow (menu-bar)]
      [:div.column
       [:div#main]]]]]])

(def introduction-text (inline-slurp "doc/introduction.md"))
(def introduction-md (marked/parse introduction-text))
(defn introduction []
  [:div.content
   (profane "p" introduction-md)])

;; VIEWS

(defn readme-view [node _hash]
  (.replaceChildren node (alchemize (introduction))))

;; MAIN

(def route->view
  {:landing       readme-view
   ;:player-guide  player-guide-view
   ;:new-character new-character-view
   ;:characters    characters-view
   ;:campaigns     campaigns-view
   ;:search        search-view
   })

(def ROUTES
  (reduce
   (fn [acc [route view]]
     (assoc acc (route->hash route) view))
   {}
   route->view))

(defn setup []
  #_(db/setup-db)
  (js/Promise.resolve))

(defn start-app [node]
  (.appendChild node (alchemize (container)))
  (let [refresh (partial handle-refresh ROUTES "main" :landing)]
    (js/window.addEventListener "popstate" refresh)
    (.then (setup) #(refresh))))

;; COMPONENTS

(defclass MainComponent
  (extends js/HTMLElement)
  (constructor [this] (super))
  Object
  (connectedCallback [this] (start-app this)))

(def components
  {:main-component MainComponent})

;; MAIN

(defn define-components []
  (doseq [[component-kw component] components]
    (js/customElements.define (name component-kw) component)))

(try
  (define-components)
  (catch js/Object e
    ;; redefining custom elements is impossible
    ;; so if webcomponents complains about dev trying to do so, reload
    ;; but otherwise, just print the error
    (if (ends-with? (ex-message e) "has already been defined as a custom element")
      (js/window.location.reload)
      (js/console.log e))))
