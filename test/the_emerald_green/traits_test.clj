(ns the-emerald-green.traits-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is testing]]
   [the-emerald-green.core :as core]
   [the-emerald-green.test-utils :refer [stest-symbols!]]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.utils :as utils]))

(deftest fspec-test
  (stest-symbols! [`traits/rule-matches-card?
                   `traits/rule-matches-cards?
                   `traits/rule-matches-traits?
                   `traits/determine-traits]))

(deftest all-traits-valid
  (doseq [trait traits/traits]
    (testing (str "Valid? " (:name trait))
      (is (s/valid? ::traits/trait* trait)
          (s/explain-str ::traits/trait* trait)))))

(deftest all-abilities-valid
  (doseq [ability* traits/abilities
          :let [{ability-name :name :as ability}
                (if (keyword? ability*)
                  (get traits/id->ability ability* {})
                  ability*)]]
    (testing (str "Valid? " ability-name)
      (is (some? ability-name)
          (when (nil? ability-name)
           (str (utils/keyword->name ability*) " is not defined anywhere!")))
      (is (s/valid? ::core/ability ability)
          (s/explain-str ::core/ability ability)))))
