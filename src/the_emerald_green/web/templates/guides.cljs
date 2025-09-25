(ns the-emerald-green.web.templates.guides
  (:require ["marked" :as marked]
            [the-emerald-green.web.alchemy :refer [profane]]
            [the-emerald-green.guides :as guides]))

(defn guide-text [text]
  (profane "span" (marked/parse text)))

(def guides
  (reduce
   (fn [acc [key text]] (assoc acc key (guide-text text)))
   {}
   (dissoc guides/guides :introduction)))

(def carla-credit "Illustration of the Fae Queen's Court by Carla Romero")
(def introduction
  [:div
   [:div.block
    [:h1.title.is-2>strong "Welcome, mortal..."]
    [:p.subtitle.is-4>em "Won't you play? Won't you dance?"]]
   [:div.block.has-text-centered
    [:figure.image.is-inline-block
     [:img
      {:src "./img/The_Emerald_Green.png"
       :alt carla-credit
       :title carla-credit}]]
    [:p.help
     "Illustration by "
     [:a {:href "https://carla-a-romero.com/"
          :target "_blank"}
      "Carla Romero"]]]
   [:div.block
    (guide-text (:introduction guides/guides))]])
