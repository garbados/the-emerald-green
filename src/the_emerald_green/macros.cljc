(ns the-emerald-green.macros)

(def resource-dir "resources")
(def sep "/")

(defn join [& args]
  (str (clojure.string/join sep (cons resource-dir args)) sep))

;; macros run in clj, during compilation
;; so cljs can use slurp
;; so long as it uses it at compilation

(defmacro inline-slurp [path]
  (clojure.core/slurp (eval path)))

(defmacro slurp-resource [path]
  (eval `(inline-slurp (join ~path))))

(defmacro slurp-edn [path]
  (clojure.edn/read-string
   (eval `(slurp-resource ~path))))

(defmacro slurp-dir-edn [path]
  (->> (join (eval path))
       clojure.java.io/file
       clojure.core/file-seq
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
