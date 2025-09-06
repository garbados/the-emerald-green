(ns the-emerald-green.core-test
  (:require
   [clojure.string :as string]
   [clojure.test :refer [deftest]]
   [the-emerald-green.test-utils :refer [stest-ns!]]
   [the-emerald-green.challenges]))

(def except-ns #{'the-emerald-green.macros
                 'the-emerald-green.test-utils})

(deftest all-ns-test
  (doall
   (->> (all-ns)
        (map ns-name)
        (map name)
        (filter #(string/starts-with? % "the-emerald-green"))
        (filter #(not (string/ends-with? % "-test")))
        (map symbol)
        (filter #(not (contains? except-ns %)))
        (map stest-ns!))))
