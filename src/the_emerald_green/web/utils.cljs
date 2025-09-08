(ns the-emerald-green.web.utils 
  (:require
   [cljs.pprint :as pprint]
   [the-emerald-green.web.alchemy :refer [refresh]]))

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
