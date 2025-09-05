(ns the-emerald-green.web.app
  (:require
   [clojure.string :refer [ends-with?]]
   [shadow.cljs.modern :refer (defclass)]
   [the-emerald-green.web.alchemy :refer [alchemize]]
   [the-emerald-green.web.db :as db]
   [the-emerald-green.web.routing :refer [handle-refresh route->hash]]
   [the-emerald-green.web.templates.guides :as guides]
   [the-emerald-green.web.templates.layout :refer [container]]
   [the-emerald-green.web.templates.tools :as tools]
   [the-emerald-green.web.templates.traits :refer [traits-guide]]
   [the-emerald-green.web.utils :refer [refresh-node static-view]]))

;; VIEWS

(def db (db/init-db "the-emerald-green"))

(defn new-character-view [node _hash]
  (.replaceChildren node (alchemize (tools/new-character))))

(defn characters-view [node _hash]
  (.replaceChildren node (alchemize (tools/characters))))

(defn campaigns-view [node _hash]
  (.replaceChildren node (alchemize (tools/campaigns))))

(defn search-view [node _hash]
  (let [query js/document.location.hash]
    (.replaceChildren node (alchemize (tools/search query)))))

;; pairing routes to views

(def route->view
  {:introduction    (static-view guides/introduction)
   :player-guide    (static-view guides/player-guide)
   :trait-guide     #(refresh-node "main" traits-guide)
   :equipment-guide (static-view guides/equipment-guide)
   :setting-guide   (static-view guides/setting-guide)
   :gm-guide        (static-view guides/gm-guide)
   :new-character   new-character-view
   :characters      characters-view
   :campaigns       campaigns-view
   :search          search-view})

(defn sanity-check-routes
  "Report routes that are:
   - Orphaned (views without a hash)
   - Unimplemented (hashes without a view)"
  []
  (concat
   (for [route (keys route->view)
         :let [url-hash (route->hash route)]
         :when (nil? url-hash)]
     (str "Orphan route: " route))
   (for [route (keys route->hash)
         :let [view (route->view route)]
         :when (nil? view)]
     (str "Unimplemented route: " route))))

(def hash->view
  (reduce
   (fn [acc [route view]]
     (assoc acc (route->hash route) view))
   {}
   route->view))

;; prepare main view

(defn setup []
  (db/setup-db db))

(defn main-view [node]
  ;; FIXME: put this in a test suite lol
  (if-let [route-errors (seq (sanity-check-routes))]
    (->> [:div.content
          [:h1 "Error!"]
          [:ul
           (for [error-msg route-errors]
             [:li error-msg])]]
         (alchemize)
         (.replaceChildren node))
    (do
      (.appendChild node (alchemize container))
      (let [refresh (partial handle-refresh hash->view "main" :introduction)]
        (js/window.addEventListener "popstate" refresh)
        (.then
         (js/Promise.resolve (setup))
         #(refresh))))))

;; webcomponents

(defclass MainComponent
  (extends js/HTMLElement)
  (constructor [this] (super))
  Object
  (connectedCallback [this] (main-view this)))

(def components
  {:main-component MainComponent})

;; main

(defn start-app []
  (try
    (doseq [[component-kw component] components]
      (js/customElements.define (name component-kw) component))
    (catch js/Object e
      ;; redefining custom elements is impossible
      ;; so if webcomponents complains about dev trying to do so, reload
      ;; but otherwise, just print the error
      (if (ends-with? (ex-message e) "has already been defined as a custom element")
        (js/window.location.reload)
        (js/console.log e)))))

(start-app)
