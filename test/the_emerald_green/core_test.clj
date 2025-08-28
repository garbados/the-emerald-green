(ns the-emerald-green.core-test
  (:require [clojure.test :refer [deftest]]
            [the-emerald-green.core :as core]
            [the-emerald-green.test-utils :refer [stest-symbols!]]))

(deftest fspec-test
  (stest-symbols! [`core/gen-deck
                   `core/remove-card
                   `core/list-missing-cards]))
