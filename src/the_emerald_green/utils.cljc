(ns the-emerald-green.utils
  (:require
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as g]
   [clojure.string :as string]))

(defn name->keyword [s]
  (keyword (string/lower-case (string/replace s " " "-"))))

(s/fdef name->keyword
  :args (s/cat :s string?)
  :ret keyword?)

(defn keyword->name [kw]
  (string/join " " (map string/capitalize (string/split (name kw) #"-"))))

(s/fdef keyword->name
  :args (s/cat :kw keyword?)
  :ret string?)

(defn idify [thing]
  (if (and (map? thing) (nil? (:id thing)))
    (assoc thing :id (-> thing :name name->keyword))
    thing))


(s/def :thing/id keyword?)
(s/def :thing/name string?)
(s/def ::thing
  (s/keys :req-un [:thing/name]
          :opt-un [:thing/id]))
(s/def ::thing-with-id
  (s/keys :req-un [:thing/name
                   :thing/id]))
(s/fdef idify
  :args (s/cat :thing ::thing)
  :ret ::thing-with-id)

(defn merge-by-id [acc {id :id :as thing}]
  (assoc acc id thing))

(s/fdef merge-by-id
  :args (s/cat :acc map?
               :thing (s/keys :req-un [:thing/id]))
  :ret map?)

(defn all-defs [coll]
  (->> coll
       (filter some?)
       flatten))

(s/def ::coll (s/coll-of any?))

(s/fdef all-defs
  :args (s/cat :coll ::coll)
  :ret (s/coll-of some?))

(defn uniq-defs [coll]
  (filter map? (all-defs coll)))

(s/fdef uniq-defs
  :args (s/cat :coll ::coll)
  :ret (s/coll-of map?))

(defn s-format
  "Replace `%s` indicators with string arguments"
  [s arg & args]
  (reduce
   #(string/replace-first %1 #"%s" (str %2))
   s
   (cons arg args)))

(s/fdef s-format
  :args (s/cat :s string?
               :args
               (s/+ (s/or :s string?
                          :n number?)))
  :ret string?)

(s/def :re/pattern
  (s/with-gen
    any?
    #(g/fmap re-pattern (g/string))))

(defn refine-extensions [id->thing thing]
  (if-let [parent (-> thing :extends id->thing)]
    (merge (refine-extensions id->thing parent) thing)
    thing))
