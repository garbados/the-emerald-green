(ns the-emerald-green.traits-test
  (:require [clojure.test :refer [deftest testing is]]
            [the-emerald-green.traits :as traits]
            [the-emerald-green.test-utils :refer [stest-symbols!]]
            [clojure.spec.alpha :as s]))

(deftest fspec-test
  (stest-symbols! [`traits/req-matches-card?
                   `traits/rule-matches-cards?]))

(deftest all-traits-valid
  (doseq [trait traits/traits]
    (testing (str "Valid requires? " (:name trait))
      (is (s/valid? ::traits/card (:requires trait))
          (s/explain-str ::traits/card (:requires trait))))
    (testing (str "Valid? " (:name trait))
      (is (s/valid? ::traits/trait* trait)
          (s/explain-str ::traits/trait* trait)))))