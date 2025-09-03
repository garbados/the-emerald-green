(ns the-emerald-green.characters-test
  (:require
   [clojure.test :refer [deftest]]
   [the-emerald-green.test-utils :refer [stest-symbols!]]
   [the-emerald-green.characters :as characters]))

(deftest fspec-test
  (stest-symbols! [`characters/determine-stats
                   #_`characters/merge-stats
                   #_`characters/merge-fungibles
                   #_`characters/reset-fungibles
                   #_`characters/ceil-fungibles
                   #_`characters/hydrate-character
                   #_`characters/dehydrate-character]))
