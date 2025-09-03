(ns the-emerald-green.money-test
  (:require
   [clojure.test :refer [deftest]]
   [the-emerald-green.test-utils :refer [stest-symbols!]]
   [the-emerald-green.money :as money]))

(deftest fspec-test
  (stest-symbols! [`money/gold-to-wealth
                   `money/wealth-to-gold
                   `money/->cost]))
