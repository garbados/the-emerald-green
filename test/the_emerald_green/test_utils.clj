(ns the-emerald-green.test-utils
  (:require [clojure.test :refer [is testing]]
            [clojure.spec.test.alpha :as stest]))

(defn stest-symbols!
  ([syms]
   (stest-symbols! syms {:num-tests 100}))
  ([syms opts]
   (doseq [sym syms]
     (testing (str sym)
       (println (str "Testing: " sym))
       (let [{:keys [failure] :as check}
             (->> (stest/check sym {:clojure.spec.test.check/opts opts})
                  (map stest/abbrev-result)
                  (filter :failure)
                  first)]
         (is (nil? failure) check))))))
