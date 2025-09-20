(ns the-emerald-green.web.utils 
  (:require
   [cljs.pprint :as pprint]
   [the-emerald-green.web.alchemy :refer [refresh snag]]))

(defn get-rect [elem]
  (js->clj (.getBoundingClientRect elem) :keywordize-keys true))

(defn scroll-to [elem]
  (let [rect (get-rect elem)]
    (js/window.scrollTo (clj->js {:left (.-left rect)
                                  :top (.-top rect)
                                  :behavior "smooth"}))))

(defn scroll-to-top []
  (js/window.scrollTo (clj->js {:left 0
                                :top 0
                                :behavior "smooth"})))

(defn scroll-to-id [id]
  (scroll-to (snag id)))

(defn refresh-node [node-ish expr]
  (refresh node-ish (clj->js (expr))))

(defn dynamic-view [expr]
  #(refresh-node % expr))

(defn static-view [template]
  #(refresh %1 (clj->js template)))

(defn pprint [thing]
  (binding [pprint/*print-right-margin* 30]
    (pprint/pprint thing)))

(def lolraw #(vec [:pre>code {:style "white-space: pre-wrap;"} (with-out-str (pprint %))]))

(def default-wait-ms 300)

(defn now-ms [] (js/Date.now))

(defn debounce [f wait-ms]
  (let [-timer (atom nil)
        clear-timeout #(js/clearTimeout %)
        set-timeout #(js/setTimeout % wait-ms)]
    (fn [& args]
      (when-let [timer @-timer]
        (clear-timeout timer))
      (reset! -timer (set-timeout #(apply f args))))))

(def immutable #{:abstract :type :id :extends})

(defn atomify [thing & {:as defaults}]
  (into {}
        (for [prop (concat (keys thing) (keys defaults))
              :let [value (get thing prop)
                    default (get defaults prop)
                    transform? (fn? default)]
              :when (not (contains? immutable prop))]
          [(keyword (str "-" (name prop)))
           (atom (if transform? (default value) (or value default)))])))

(defn marshal-thing [atoms & {:as transforms}]
  (into {:abstract false}
        (concat
         (for [[prop* -value] atoms
               :let [prop (keyword (subs (name prop*) 1))
                     value
                     (if-let [transform (get transforms prop)]
                       (transform @-value)
                       @-value)]
               :when (cond
                       (string? value) (seq value)
                       :else value)]
           [prop value])
         (for [[prop transform] transforms
               :when (not (get atoms (keyword (str "-" (name prop)))))]
           [prop (transform)]))))

(def markdown-tip
  [:em "Use " [:a {:href "https://commonmark.org/help/" :target "_blank"} "Markdown!"]])
