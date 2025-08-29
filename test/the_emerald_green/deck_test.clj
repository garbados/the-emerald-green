(ns the-emerald-green.deck-test
  (:require [clojure.test :refer [deftest testing is]]
            [the-emerald-green.deck :as deck]
            [the-emerald-green.test-utils :refer [stest-symbols!]]
            [clojure.spec.alpha :as s]))

(deftest fspec-test
  (stest-symbols! [`deck/gen-deck
                   `deck/arcana-name->keyword
                   `deck/remove-card
                   `deck/remove-cards-by-tag
                   `deck/list-missing-cards]))

(deftest base-deck-valid
  (doseq [card deck/base-deck]
    (testing (str "Valid? " (:name card))
      (is (s/valid? ::deck/card* card) (s/explain-str ::deck/card* card)))))
