(ns the-emerald-green.money 
  (:require [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as g]
            [clojure.string :as string]))

(def x-copper   1)
(def x-silver   (* 100 x-copper))
(def x-gold     (* 100 x-silver))
(def x-platinum (* 100 x-gold))

(def reasonable-coinage 10000)
(s/def ::wealth (s/int-in 0 (* x-platinum reasonable-coinage)))

(def gold-expr #"^([\d,]+p)?([\d,]+g)?([\d,]+s)?([\d,]+c)?$")
(s/def ::gold-ish
  (s/with-gen
    (s/and string? (partial re-matches gold-expr))
    #(g/fmap
      (fn [[p g s c]]
        (str (when p (str p "p"))
             (when g (str g "g"))
             (when s (str s "s"))
             (when c (str c "c"))))
      (s/gen (s/tuple (s/nilable (s/int-in 0 (inc reasonable-coinage)))
                      (s/nilable (s/int-in 0 reasonable-coinage))
                      (s/nilable (s/int-in 0 reasonable-coinage))
                      (s/nilable (s/int-in 0 reasonable-coinage)))))))

(defn wealth-to-gold [wealth]
  (cond-> ""
    (>= wealth x-platinum) (str (int (/ wealth x-platinum)) "p")
    (> (rem wealth x-platinum) x-gold) (str (int (/ (rem wealth x-platinum) x-gold)) "g")
    (> (rem wealth x-gold) x-silver) (str (int (/ (rem wealth x-gold) x-silver)) "s")
    (pos-int? (rem wealth x-silver)) (str (int (rem wealth x-silver)) "c")))

(s/fdef wealth-to-gold
  :args (s/cat :wealth ::wealth)
  :ret ::gold-ish)

(defn gold-to-wealth [gold-ish]
  (if (seq gold-ish)
    (when-let [match (re-matches gold-expr (string/replace gold-ish #"," ""))]
      (->> (for [[coin weight] (map vector (rest match) [x-platinum x-gold x-silver x-copper])
                 :when coin
                 :let [wealth ((comp
                                parse-long
                                #(string/replace % #"," "")
                                second
                                (partial re-matches #"^(\d+)[pgsc]$"))
                               coin)]]
             (* wealth weight))
           (reduce + 0)))
    0))

(s/fdef gold-to-wealth
  :args (s/cat :gold-ish ::gold-ish)
  :ret nat-int?)

(defn ->cost [x]
  (if (string? x)
    (gold-to-wealth x)
    x))

(s/def ::cost
  (s/or :gold-ish ::gold-ish
        :wealth ::wealth))

(s/fdef ->cost
  :args (s/cat :x ::cost))
