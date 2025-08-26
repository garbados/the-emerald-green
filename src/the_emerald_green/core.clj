(ns the-emerald-green.core
  (:require [clojure.spec.alpha :as s]
            [clj-uuid :as uuid]))

(defn my-add
  "Add numbers like + but just to test fn instrumentation."
  [& xs]
  (reduce + 0 xs))

(s/fdef my-add
  :args (s/+ number?)
  :ret number?)

(def possible-colors #{:red :green :blue})
(def min-height 60)
(def max-height 72)

(s/def ::id uuid?)
(s/def ::favorite-color possible-colors)
(s/def ::height (s/int-in min-height (inc max-height)))
(s/def ::user
  (s/keys :req-un [::id
                   ::favorite-color
                   ::height]))

(defn gen-user []
  {:id (random-uuid)
   :favorite-color (rand-nth possible-colors)
   :height (+ min-height (rand-int (inc (- max-height min-height))))})

(defn infinite-users
  ([] (infinite-users (gen-user)))
  ([user] (lazy-seq (cons user (infinite-users (gen-user))))))

(defn inch->cm [x]
  (* x 2.54))

(defn average [xs]
  (/ (reduce + 0 xs) (count xs)))

(defn gen-user-from-seed [seed]
  {:id (uuid/v4 0 seed)
   :favorite-color (nth (seq possible-colors) (rem seed (count possible-colors)))
   :height (+ min-height (rem seed (inc (- max-height min-height))))})
