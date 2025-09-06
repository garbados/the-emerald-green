(ns the-emerald-green.web.db
  (:require ["pouchdb" :as pouchdb]))

(defn init-db
  ([name]
   (new pouchdb name))
  ([name opts]
   (new pouchdb name opts)))

(def db (init-db "the-emerald-green"))

(defn setup-db []
  (-> (js/Promise.all
       (clj->js
        []))
      (.then #(.allDocs db (clj->js {:include_docs false})))
      (.then #(do
                (js/console.log "DB OK! Contents:")
                (js/console.log %)))))
