(ns the-emerald-green.deck-test
  (:require [clojure.test :refer [deftest testing is]]
            [the-emerald-green.deck :as deck]
            [clojure.spec.alpha :as s]))

(deftest base-deck-valid
  (doseq [card deck/base-deck]
    (testing (str "Valid? " (:name card))
      (is (s/valid? ::deck/card* card)
          (s/explain-str ::deck/card* card)))))
