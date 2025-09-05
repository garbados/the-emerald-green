(ns the-emerald-green.web.templates.traits
  (:require
   ["marked" :as marked]
   [clojure.pprint :as pprint]
   [clojure.string :as string]
   [the-emerald-green.traits :as traits]
   [the-emerald-green.web.alchemy :refer [profane]]
   [the-emerald-green.web.prompts :as prompts]
   [the-emerald-green.web.utils :refer [refresh-node]]))

(defn print-trait
  ([{trait-name :name
     trait-reqs :traits
     deck-reqs :deck
     card-reqs :card
     :keys [description]}
    & [n]]
   [:div.card
    [:div.card-content>div.content
     [:p.subtitle trait-name (when (< 1 n) (str " (x " n ")"))]
     (profane "p" (marked/parse description))
     [:p "Requires:"]
     [:ul
      (for [[reqs title] [[trait-reqs "Traits"]
                          [deck-reqs "Deck"]
                          [card-reqs "Card"]]
            :when reqs]
        [:li
         [:p title]
         [:p [:pre>code (with-out-str (pprint/pprint reqs))]]
        ;;  using hljs to highlight edn -- like 300kb deps for some vaguely readable colored text. consider it? i guess?
         #_[:p [:pre (profane "code" (.-value (js/hljs.highlight "clojure" (with-out-str (pprint/pprint reqs)))))]]])]]]))

(defn join-reqs [reqs]
  (if (keyword? reqs)
    (name reqs)
    (->> reqs flatten set (filter keyword?) (map name) sort (string/join ", "))))

(defn list-traits
  ([] (list-traits ""))
  ([query] (list-traits query traits/traits))
  ([query traits]
   (let [re (re-pattern query)]
     [:div
      (if (seq query)
        (->> traits
             (filter #(or (when (:card %) (re-find re (join-reqs (:card %))))
                          (when (:deck %) (re-find re (join-reqs (:deck %))))
                          (when (:traits %) (re-find re (join-reqs (:traits %))))
                          (re-find re (string/lower-case (:name %)))
                          (re-find re (string/lower-case (:description %)))))
             (map print-trait))
        (map print-trait traits))])))

(defn traits-guide []
  (let [-query (atom "")
        refresh-traits #(refresh-node "traits" (partial list-traits @-query))]
    (add-watch -query :query refresh-traits)
    [:div.content
     [:h1 "Trait Guide"]
     [:p "Here are documented all the fae traits you may... develop."]
     (prompts/text -query
                   :placeholder "🔍 Filter cards by name, description, or requirements.")
     [:hr]
     [:div#traits (list-traits)]]))
