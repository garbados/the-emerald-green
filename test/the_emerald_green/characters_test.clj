(ns the-emerald-green.characters-test
  (:require
   [clojure.spec.alpha :as s]
   [clojure.test :refer [deftest is testing]]
   [clojure.test.check.clojure-test :refer [defspec]]
   [clojure.test.check.properties :as properties]
   [the-emerald-green.characters :as characters]
   [the-emerald-green.test-utils :refer [stest-symbols!]]))

(deftest fspec-test
  (stest-symbols! [`characters/determine-stats
                   `characters/merge-stats
                   `characters/merge-fungibles
                   `characters/reset-fungibles
                   `characters/ceil-fungibles
                   `characters/hydrate-character
                   `characters/dehydrate-character]))

(defspec test-print-character 100
  (properties/for-all [character (s/gen ::characters/character)]
    (let [result (with-out-str (characters/print-character character))]
      (is (string? result)
          (str "Expected string, got " (type result))))))

(deftest test-example-characters
  (testing "Valid example characters?"
    (doseq [character characters/examples]
      (is (s/valid? ::characters/character character)
          (s/explain-str ::characters/character character)))))
