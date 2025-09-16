(ns the-emerald-green.macros)

(def resource-dir "resources")
(def sep "/")

(defn join [& args]
  #_{:clj-kondo/ignore [:unresolved-namespace]}
  (str (clojure.string/join sep (cons resource-dir args)) sep))

;; macros run in clj, during compilation
;; so cljs can use slurp
;; so long as it uses it at compilation

(defmacro inline-slurp [path]
  #_{:clj-kondo/ignore [:unresolved-var]}
  (clojure.core/slurp (eval path)))

(defmacro slurp-resource [path]
  (eval `(inline-slurp (join ~path))))

(defmacro slurp-edn [path]
  #_{:clj-kondo/ignore [:unresolved-namespace]}
  (clojure.edn/read-string
   (eval `(slurp-resource ~path))))

(defmacro slurp-dir-edn [path]
  #_{:clj-kondo/ignore [:unresolved-namespace :unresolved-var]}
  (->> (join (eval path))
       clojure.java.io/file
       clojure.core/file-seq
       (filter #(.isFile %))
       (map #(.getPath %))
       (filter (partial re-matches #"^.*?\.edn$"))
       (map clojure.core/slurp)
       (map clojure.edn/read-string)
       (reduce into [])))
