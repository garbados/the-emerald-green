(ns the-emerald-green.web.prompts
  (:require
   [the-emerald-green.utils :refer [keyword->name name->keyword]]
   [the-emerald-green.web.utils :refer [debounce default-wait-ms]]))

(defn input
  [-value
   & {:keys [on-submit on-change
             placeholder wait
             options-list input-type
             props]
      :or {wait default-wait-ms}}]
  (let [oninput (debounce #(reset! -value (-> % .-target .-value)) wait)
        onkeydown
        #(cond
           (and on-submit (= 13 (.-which %))) (on-submit @-value)
           on-change (on-change @-value))]
    [:input.input
     (cond->
      {:type input-type
       :value @-value
       :oninput oninput}
       placeholder              (assoc :placeholder placeholder)
       (or on-submit on-change) (assoc :onkeydown onkeydown)
       options-list             (assoc :list options-list)
       props                    (merge props))]))

(defn text [-value & args]
  (apply input -value :input-type "text" args))

(defn textarea [-value & {:keys [wait on-submit] :or {wait default-wait-ms}}]
  (let [onkeydown
        (when on-submit
          #(when (and (or (= 10 (.-keyCode %))
                          (= 13 (.-keyCode %)))
                      (.-ctrlKey %))
             (on-submit @-value)))]
    [:textarea.textarea
     {:oninput (debounce #(reset! -value (-> % .-target .-value)) wait)
      :onkeydown onkeydown
      :rows 10}
     @-value]))

(defn number [-value & args]
  (apply input -value :input-type "number" args))

(defn choose-one [-choice options & {:keys [wait] :or {wait default-wait-ms}}]
  [:div.select
   [:select
    {:oninput (debounce #(reset! -choice (-> % .-target .-value name->keyword)) wait)}
    (for [option (cons @-choice options)]
      [:option (keyword->name option)])]])

(defn autocomplete-one
  "Input for completing a single value.
  Uses a `<datalist>` on a typical `<input>`."
  [-value options & args]
  (let [options-id (str "autocomplete-" (random-uuid))]
    [(apply text -value :options-list options-id args)
     [(str "datalist#" options-id)
      (for [option options]
        [:option {:value option}])]]))

;; TODO FIXME: this is actually obviously behaviorally distinct
;; - watch variable
;; - take last comma-sep'd section
;; - present filtered options
;; - update to replace last comma-sep'd section
(def autocomplete-many autocomplete-one)

(defn field [label help prompt -atom & args]
  [[:label.label label]
   (when help
     [:p.help help])
   [:div.field
    [:div.control (apply prompt -atom args)]]])
