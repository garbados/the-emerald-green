(ns the-emerald-green.web.app-test 
  (:require
   [cljs.test :refer [deftest testing is]]
   [the-emerald-green.web.app :refer [route->view]]
   [the-emerald-green.web.routing :refer [route->hash]]))

(deftest routes-test
  "Report routes that are:
   - Orphaned (views without a hash)
   - Unimplemented (hashes without a view)"
  (doseq [route (->> [route->hash route->view]
                     (map keys)
                     (reduce into #{}))
          :let [url-hash (route->hash route)
                view (route->view route)]]
    (testing (str "route: " (name route))
      (is (some? url-hash)
          (str "Orphan route: " route))
      (is (some? view)
          (str "Unimplemented route: " route)))))
