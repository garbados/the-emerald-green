(ns splash-interview-project.utils)

;; macros run in clj, during compilation
;; so cljs can use slurp
;; so long as it uses it at compilation
(defmacro inline-slurp [path]
  (clojure.core/slurp path))

(defmacro slurp-edn [path]
  (clojure.edn/read-string
   (clojure.core/slurp path)))
