(ns splash-interview-project.core-test
  (:require [clojure.test :refer [deftest]]
            [splash-interview-project.core :as core]
            [splash-interview-project.test-utils :refer [stest-symbols!]]))

(deftest fspec-test
  (stest-symbols! [`core/my-add]))
