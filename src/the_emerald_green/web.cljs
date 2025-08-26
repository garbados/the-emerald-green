(ns the-emerald-green.web
  (:require ["html-alchemist" :refer [snag listento profane] :as alchemy]
            [shadow.cljs.modern :refer (defclass)]
            [clojure.string :refer [ends-with?]]))

;; PREAMBLE

(defn alchemize
  "Clojure-friendly wrapper around Alchemist's alchemize function."
  [expr]
  (alchemy/alchemize (clj->js expr)))

;; TEMPLATES

(def hello-world-title
  [:div.container
   [:section
    [:hgroup
     [:h1 "Hello World"]
     [:p "It's nice to meet you :)"]]]])

;; COMPONENTS

(defclass MainComponent
  (extends js/HTMLElement)
  (constructor [this] (super))
  Object
  (connectedCallback [this] (.replaceChildren this (alchemize hello-world-title))))

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
