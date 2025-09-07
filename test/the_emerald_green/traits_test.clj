(ns the-emerald-green.traits-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is testing]]
   [the-emerald-green.core :as core]
   [the-emerald-green.traits :as traits]))

(deftest all-traits-valid
  (doseq [trait traits/traits]
    (testing (str "Valid? " (:name trait))
      (is (s/valid? ::traits/trait* trait)
          (s/explain-str ::traits/trait* trait)))))

(deftest all-talents-valid
  (doseq [{talent-name :name :as talent} traits/abilities]
    (testing (str "Valid? " talent-name)
      (is (s/valid? ::core/talent talent)
          (s/explain-str ::core/talent talent)))))

(deftest all-abilities-valid
  (doseq [{ability-name :name :as ability} traits/abilities]
    (testing (str "Valid? " ability-name)
      (is (s/valid? ::core/ability ability)
          (s/explain-str ::core/ability ability)))))

(deftest req-help-test
  (doseq [req-kw (into traits/bool-reqs traits/comp-reqs)]
    (testing (str "Requirement keyword: " (name req-kw))
      (is (contains? traits/req->help req-kw)
          (str (name req-kw) " has no help description!")))))

(deftest req-comp-test
  (doseq [req-kw traits/comp-reqs]
    (testing (str "Comparator keyword: " (name req-kw))
      (is (contains? traits/req->comp req-kw)
          (str (name req-kw) " has no associated function!")))))
