(ns the-emerald-green.utils
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as string]))

(defn name->keyword [s]
  (keyword (string/lower-case (string/replace s " " "-"))))

(s/fdef name->keyword
  :args (s/cat :s string?)
  :ret keyword?)

(defn keyword->name [kw]
  (string/join " " (map string/capitalize (string/split (name kw) #"-"))))

(s/fdef keyword->name
  :args (s/cat :kw keyword?)
  :ret string?)

(defn idify [thing]
  (if (and (map? thing) (nil? (:id thing)))
    (assoc thing :id (-> thing :name name->keyword))
    thing))

(defn merge-by-id [acc {id :id :as thing}]
  (assoc acc id thing))

(defn all-defs [coll]
  (->> coll
       (filter some?)
       flatten))

(defn uniq-defs [coll]
  (filter map? (all-defs coll)))
