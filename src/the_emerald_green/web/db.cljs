(ns the-emerald-green.web.db
  (:require
   ["pouchdb" :as pouchdb]
   [clojure.edn :as edn]
   [the-emerald-green.characters :as c]))

(defn init-db
  ([name]
   (new pouchdb name))
  ([name opts]
   (new pouchdb name opts)))

(def db (init-db "the-emerald-green"))
(def edn-prop :-edn)

(defn then-print [promise]
  (.then promise
         #(js/console.log (pr-str %))))

(defn destroy-db []
  (.destroy db))

(defn unmarshal-doc [doc]
  (let [{edn edn-prop
         id_ :_id} (js->clj doc :keywordize-keys true)]
    (merge {:_id id_} (edn/read-string edn))))

(defn marshal-doc [value & {:keys [id to-index]
                            :or {id (random-uuid)
                                 to-index []}}]
  (clj->js
   (merge {:_id (str id)}
          (select-keys value to-index)
          {edn-prop (pr-str value)})))

(defn update-rev [old-doc new-doc]
  (set! (.-_rev new-doc) (.-_rev old-doc)))

(defn get-doc [id]
  (.get db id))

(defn put-doc [doc]
  (.put db doc))

(defn save-doc! [thing & args]
  (put-doc (apply marshal-doc thing args)))

(defn upsert-doc! [thing & args]
  (let [doc (apply marshal-doc thing args)
        upsert! #(do (update-rev % doc)
                     (put-doc doc))]
    (.catch
     (put-doc doc)
     #(if (= 409 (.-status %))
        (.then (get-doc (.-_id doc)) upsert!)
        (throw %)))))

(defn delete-id! [id]
  (.then (get-doc id)
         #(.remove db %)))

(defn snag-edn [id]
  (.then (get-doc id) (comp second unmarshal-doc)))

(def character-prefix "character")
(defn save-character! [character & [id]]
  (let [dehydrated (c/dehydrate-character character)]
    (upsert-doc! dehydrated :id (or id (str character-prefix "/" (random-uuid))))))

(def equipment-prefix "equipment")
(defn save-equipment! [equipment & [id]]
  (upsert-doc! equipment :id (or id (str equipment-prefix "/" (random-uuid)))))

(defn normalize-results [results docs?]
  (vec
   (for [js-row (.-rows results)
         :let [js-doc (.-doc js-row)]]
     (if docs?
       (unmarshal-doc js-doc)
       (.-id js-row)))))

(defn list-type [prefix & {:keys [docs?]
                           :or {docs? true}}]
  (let [opts (clj->js {:include_docs docs?
                       :startkey (str prefix "/")
                       :endkey (str prefix "/\uffff")})]
    (.then (.allDocs db opts) #(normalize-results % docs?))))

(defn list-characters [& args]
  (apply list-type character-prefix args))

(defn list-equipment [& args]
  (apply list-type equipment-prefix args))

(defn setup-db []
  (-> (js/Promise.all
       (clj->js
        [(list-characters)
         (list-equipment)]))
      (.then
       (fn [[characters equipment]]
         {:characters (zipmap (map :_id characters) characters)
          :equipment (zipmap (map :_id equipment) equipment)}))
      (.then
       #(do
          (js/console.log "DB OK! Contents:")
          (println %)
          (js/console.log (clj->js %))))))
