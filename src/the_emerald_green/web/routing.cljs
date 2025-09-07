(ns the-emerald-green.web.routing
  (:require
   [clojure.string :as string]
   [the-emerald-green.web.alchemy :refer [snag]]))

(def default-route :introduction)
(def not-found-route :not-found)

(def route->hash
  {:introduction       "#/introduction"
   :player-guide       "#/guides/player"
   :card-guide         "#/guides/cards"
   :trait-guide        "#/guides/traits"
   :equipment-guide    "#/guides/equipment"
   :setting-guide      "#/guides/setting"
   :gm-guide           "#/guides/gm"
   :template-stuff     "#/equipment/new-from"
   :invent-stuff       "#/equipment/new"
   :edit-stuff         "#/equipment/edit"
   :template-character "#/characters/new-from"
   :new-character      "#/characters/new"
   :edit-character     "#/characters/edit"
   :characters         "#/characters"
   :campaigns          "#/campaigns"
   :search             "#/search"
   :not-found          "#/404"})

(defn route->href [route & args]
  {:href (string/join "/" (cons (route->hash route) args))})

(defn route-pattern [route]
  (-> js/document.location.hash
      (string/replace-first (re-pattern (route->hash route)) "")
      (string/replace-first #"^/" "")))

(defn goto-str [s]
  (set! js/window.location s))

(defn goto-search [query]
  (goto-str (str (route->hash :search) "/" query)))

(defn goto [route] (goto-str (get route->hash route not-found-route)))
(def go-home #(goto default-route))

(defn redirect
  ([] (redirect default-route))
  ([route] (redirect route 1))
  ([route wait-ms]
   (js/setTimeout #(goto route) wait-ms)
   [:p "Redirecting..."]))

(defn find-view [route->view url-hash]
  (->> (keys route->view)
       (filter #(re-find (re-pattern %) url-hash))
       sort
       last
       route->view))

(defn handle-refresh [route->view main-id]
  (let [url-hash js/document.location.hash]
    (if (seq url-hash)
      (if-let [view (find-view route->view url-hash)]
        (view (snag main-id))
        (goto-str (string/replace-first url-hash #"^#/" (str (route->hash not-found-route) "/"))))
      (goto default-route))))

(defn four-oh-four [& {:keys [route not-found-msg or-try-msg or-try-route]
                       :or {route :not-found
                            not-found-msg "Uh-oh! 404!"
                            or-try-msg "go home"
                            or-try-route default-route}}]
  (let [not-found (route-pattern route)]
    (if (seq not-found)
      [:div.content
       [:h1.title not-found-msg]
       [:p.subtitle "I couldn't find that: " not-found]
       [:p "Why not " [:a (route->href or-try-route) or-try-msg] "?"]]
      (redirect))))
