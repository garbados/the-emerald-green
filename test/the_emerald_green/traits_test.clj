(ns the-emerald-green.traits-test
  (:require [clojure.test :refer [deftest testing is]]
            [the-emerald-green.traits :as traits]
            [the-emerald-green.test-utils :refer [stest-symbols!]]
            [clojure.spec.alpha :as s]))

(deftest fspec-test
  (stest-symbols! [`traits/rule-matches-card?
                   `traits/rule-matches-deck?]))

(deftest all-traits-valid
  (doseq [trait traits/all-traits]
    (testing (str "Valid requires? " (:name trait))
      (is (s/valid? ::traits/requires* (:requires trait))
          (s/explain-str ::traits/requires* (:requires trait))))
    (testing (str "Valid? " (:name trait))
      (is (s/valid? ::traits/trait* trait)
          (s/explain-str ::traits/trait* trait)))))