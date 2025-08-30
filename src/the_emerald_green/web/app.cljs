(ns the-emerald-green.web.app
  (:require
   ["html-alchemist" :refer [profane snag] :as alchemy]
   ["marked" :as marked]
   [clojure.string :as string]
   [shadow.cljs.modern :refer (defclass)]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.utils :refer-macros [inline-slurp]]
   [the-emerald-green.deck :as deck]))

;; PREAMBLE

(defn alchemize
  "Clojure-friendly wrapper around Alchemist's alchemize function."
  [expr]
  (alchemy/alchemize (clj->js expr)))

;; ROUTING

(def route->hash
  {:landing         "#/introduction"
   :player-guide    "#/guides/player"
   :trait-guide     "#/guides/traits"
   :equipment-guide "#/guides/equipment"
   :setting-guide   "#/guides/setting"
   :gm-guide        "#/guides/gm"
   :new-character   "#/characters/new"
   :characters      "#/characters"
   :campaigns       "#/campaigns"
   :search          "#/search"})

(def default-route "#/introduction") ; put a 404 here someday?

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
   [:p.menu-label "Guides"]
   [:ul.menu-list
    [:li [:a (route->href :player-guide) "📕 Player Guide"]]
    [:li [:a (route->href :trait-guide) "📗 Trait Guide"]]
    [:li [:a (route->href :equipment-guide) "📘 Equipment Guide"]]
    [:li [:a (route->href :setting-guide) "📙 Setting Guide"]]
    [:li [:a (route->href :gm-guide) "📓 GM Guide"]]]
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
       [:div.box#main]]]]]])

(def introduction-text (inline-slurp "doc/introduction.md"))
(def introduction-md (marked/parse introduction-text))
(defn introduction []
  [:div.content
   (profane "p" introduction-md)])

(def player-guide-text (inline-slurp "doc/player_guide.md"))
(def player-guide-md (marked/parse player-guide-text))
(defn player-guide []
  [:div.content
   (profane "p" player-guide-md)])

(defn print-reqs [rule]
  (cond
    (#{:and :or} (first rule))
    [:ul>li
     [:p (string/capitalize (name (first rule)))]
     (print-reqs (rest rule))]
    (= :count (first rule))
    [:ul>li
     [:p (str "Needs " (second rule))]
     (print-reqs (drop 2 rule))]
    :else
    [:ul
     (for [subrule rule]
       (if (sequential? subrule)
         [:li (string/join ", " (map deck/arcana-keyword->name subrule))]
         [:li (deck/arcana-keyword->name subrule)]))]))

(defn trait-guide []
  [:div.content
   [:h1 "Trait Guide"]
   [:p "Here are documented all the fae traits you may... develop."]
   (for [{trait-name :name
          requirements :requires
          :keys [description]} traits/all-traits]
     [:div.card
      [:div.card-content>div.content
       [:h3 trait-name]
       (profane "p" (marked/parse description))
       [:p "Requires:"]
       (print-reqs requirements)]])])

(defn equipment-guide []
  [:div.content
   [:h1 "Equipment Guide"]
   [:p "TODO"]])

(def setting-guide-text (inline-slurp "doc/setting_guide.md"))
(def setting-guide-md (marked/parse setting-guide-text))
(defn setting-guide []
  [:div.content
   (profane "p" setting-guide-md)])

(def gm-guide-text (inline-slurp "doc/gm_guide.md"))
(def gm-guide-md (marked/parse gm-guide-text))
(defn gm-guide []
  [:div.content
   (profane "p" gm-guide-md)])

(defn new-character []
  [:div.content
   [:h1 "New Character"]
   [:p "TODO"]])

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

;; VIEWS

(defn view-factory [template]
  (fn [node _hash]
    (.replaceChildren node (alchemize (template)))))

(def introduction-view (view-factory introduction))
(def player-guide-view (view-factory player-guide))
(def trait-guide-view (view-factory trait-guide))
(def equipment-guide-view (view-factory equipment-guide))
(def setting-guide-view (view-factory setting-guide))
(def gm-guide-view (view-factory gm-guide))
(def new-character-view (view-factory new-character))
(def characters-view (view-factory characters))
(def campaigns-view (view-factory campaigns))
(def search-view (view-factory search))

;; MAIN

(def route->view
  {:landing         introduction-view
   :player-guide    player-guide-view
   :trait-guide     trait-guide-view
   :equipment-guide equipment-guide-view
   :setting-guide   setting-guide-view
   :gm-guide        gm-guide-view
   :new-character   new-character-view
   :characters      characters-view
   :campaigns       campaigns-view
   :search          search-view})

(def ROUTES
  (reduce
   (fn [acc [route view]]
     (assoc acc (route->hash route) view))
   {}
   route->view))

(defn setup []
  #_(db/setup-db))

(defn start-app [node]
  (.appendChild node (alchemize (container)))
  (let [refresh (partial handle-refresh ROUTES "main" :landing)]
    (js/window.addEventListener "popstate" refresh)
    (.then
     (js/Promise.resolve (setup))
     #(refresh))))

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
    (if (string/ends-with? (ex-message e) "has already been defined as a custom element")
      (js/window.location.reload)
      (js/console.log e))))
