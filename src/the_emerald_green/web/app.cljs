(ns the-emerald-green.web.app
  (:require
   [clojure.string :as string]
   [shadow.cljs.modern :refer (defclass)]
   [the-emerald-green.web.alchemy :refer [alchemize]]
   [the-emerald-green.web.db :as db]
   [the-emerald-green.web.routing :refer [four-oh-four handle-refresh
                                          route->hash]]
   [the-emerald-green.web.templates.characters :refer [list-characters]]
   [the-emerald-green.web.templates.deck :refer [card-guide]]
   [the-emerald-green.web.templates.equipment :refer [equipment-guide]]
   [the-emerald-green.web.templates.guides :as guides]
   [the-emerald-green.web.templates.layout :refer [container]]
   [the-emerald-green.web.templates.tools :as tools]
   [the-emerald-green.web.templates.traits :refer [traits-guide]]
   [the-emerald-green.web.utils :refer [dynamic-view static-view]]
   [the-emerald-green.web.views.characters :refer [edit-character
                                                   edit-custom-character
                                                   template-character]]
   [the-emerald-green.web.views.equipment :refer [design-equipment
                                                  edit-equipment from-template]]))

;; CONSTANTS

(def main-id "main") ; contained in layout.cljs, see `container`

;; VIEWS

(def route->view
  (merge
   (reduce
    (fn [acc [route view]] (assoc acc route (dynamic-view view)))
    {}
    {:card-guide         card-guide
     :trait-guide        traits-guide
     :equipment-guide    equipment-guide
     :characters         #(list-characters)
     :template-character #(template-character)
     :new-character      #(edit-character :new? true)
     :edit-character     #(edit-custom-character)
     :campaigns          tools/campaigns
     :search             tools/search
     :template-stuff     from-template
     :invent-stuff       design-equipment
     :edit-stuff         edit-equipment
     :not-found          four-oh-four})
   (reduce
    (fn [acc [route template]] (assoc acc route (static-view template)))
    {}
    guides/guides)))

(def hash->view
  (zipmap (map route->hash (keys route->view)) (vals route->view)))

;; prepare main view

(defn setup []
  (db/setup-db))

(defn main-view [node]
  (.appendChild node (alchemize container))
  (let [refresh (partial handle-refresh hash->view main-id)]
    (js/window.addEventListener "popstate" refresh)
    (.then
     (js/Promise.resolve (setup))
     #(refresh))))

;; webcomponents

(defclass MainComponent
  (extends js/HTMLElement)
  (constructor [this] (super))
  Object
  (connectedCallback [this] (main-view this)))

(def components
  {:main-component MainComponent})

;; main

(def already-defined? "has already been defined as a custom element")
(defn- start-app []
  (try
    (doseq [[component-kw component] components]
      (js/customElements.define (name component-kw) component))
    (catch js/Object e
      ;; redefining custom elements is impossible
      ;; so if webcomponents complains about dev trying to do so, reload
      ;; but otherwise, just print the error
      (if (string/ends-with? (ex-message e) already-defined?)
        (js/window.location.reload)
        (js/console.log e)))))

(start-app)
