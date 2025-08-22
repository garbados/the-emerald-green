(ns splash-interview-project.core
  (:require [clojure.spec.alpha :as s]))

(defn my-add
  "Add numbers like + but just to test fn instrumentation."
  [& xs]
  (reduce + 0 xs))

(s/fdef my-add
  :args (s/+ number?)
  :ret number?)
