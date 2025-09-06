(ns the-emerald-green.web.utils 
  (:require
   [the-emerald-green.web.alchemy :refer [refresh]]))

(defn refresh-node [node-ish expr]
  (refresh node-ish (clj->js (expr))))

(defn dynamic-view [expr]
  #(refresh %1 (clj->js (expr))))

(defn static-view [template]
  #(refresh %1 (clj->js template)))
