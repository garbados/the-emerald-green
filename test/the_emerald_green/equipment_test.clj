(ns the-emerald-green.equipment-test 
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is testing]]
   [the-emerald-green.equipment :as equipment]))

(deftest equipment-test
  (doseq [equipment equipment/equippable]
    (testing (str "Valid? " (:name equipment))
      (is (s/valid? ::equipment/equipment* equipment)
          (s/explain-str ::equipment/equipment* equipment)))))
