(ns the-emerald-green.utils)

;; macros run in clj, during compilation
;; so cljs can use slurp
;; so long as it uses it at compilation
(defmacro inline-slurp [path]
  (clojure.core/slurp path))

(defmacro slurp-edn [path]
  (clojure.edn/read-string
   (clojure.core/slurp path)))

(defmacro slurp-dir-edn [dir-path]
  (->> (clojure.core/file-seq (clojure.java.io/file dir-path))
       (filter #(.isFile %))
       (map #(.getPath %))
       (filter (partial re-matches #"^.*?\.edn$"))
       (map clojure.core/slurp)
       (map clojure.edn/read-string)
       (reduce into [])))

(defmacro sdef-match-syntax [reqspec idspec]
  `(s/def ~reqspec
     (s/or :and-or
           (s/cat :pred #{:or :and}
                  :expr (s/+ ~reqspec))
           :count
           (s/cat :pred #{:count}
                  :n pos-int?
                  :expr (s/+ ~reqspec))
           :expr (s/coll-of ~idspec :kind vector?)
           :solo ~idspec)))
