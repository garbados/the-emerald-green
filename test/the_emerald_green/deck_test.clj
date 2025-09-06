(ns the-emerald-green.deck-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.string :as string]
   [clojure.test :refer [deftest is testing]]
   [the-emerald-green.deck :as deck]))

(deftest base-deck-valid
  (doseq [card deck/base-deck]
    (testing (str "Valid? " (:name card))
      (is (s/valid? ::deck/card* card)
          (s/explain-str ::deck/card* card)))))

(deftest the-order-of-things-test
  (testing "The order of things includes all things."
    (let [remaining (reduce disj (set (map :id deck/base-deck)) deck/the-order-of-things)]
      (is (nil? (seq remaining))
          (str "Not included: " (string/join ", " (map name remaining))))))
  (testing "All things encompass the order."
    (let [remaining (reduce disj (set deck/the-order-of-things) (map :id deck/base-deck))]
      (is (nil? (seq remaining))
          (str "Not included: " (string/join ", " (map name remaining)))))))
