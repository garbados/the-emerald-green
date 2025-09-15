(ns the-emerald-green.web.prompts
  (:require
   [the-emerald-green.utils :refer [keyword->name name->keyword]]
   [the-emerald-green.web.utils :refer [debounce default-wait-ms refresh-node]]))

(defn text [-value & {:keys [on-submit on-change placeholder wait]
                      :or {wait default-wait-ms}}]
  (let [oninput (debounce #(reset! -value (-> % .-target .-value)) wait)
        onkeydown
        #(cond
           (and on-submit (= 13 (.-which %))) (on-submit @-value)
           on-change (on-change @-value))]
    [:input.input
     (cond->
      {:type "text"
       :value @-value
       :oninput oninput}
       placeholder (assoc :placeholder placeholder)
       (or on-submit on-change) (assoc :onkeydown onkeydown))]))

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

(defn choose-one [-choice options & {:keys [wait] :or {wait default-wait-ms}}]
  [:div.select
   [:select
    {:oninput (debounce #(reset! -choice (-> % .-target .-value name->keyword)) wait)}
    (for [option options]
      [:option (keyword->name option)])]])

(defn field [label help prompt -atom & args]
  [:div.field
   [:label.label label]
   [:div.control (apply prompt -atom args)]
   (when help
     [:p.help help])])

(defn list-dropdown [matches on-select]
  [:p.dropdown-content
   (cond-> {}
     (nil? (seq matches)) (merge {:style "display: none;"}))
   (for [match matches]
     [:button.is-link.dropdown-item
      {:onclick #(on-select match)}
      (:name match match)])])

;; crib https://codesandbox.io/p/sandbox/bulma-autocomplete-gm3pd?file=%2Fsrc%2FAutocomplete.jsx%3A105%2C17
(defn dropdown [-value search-fn on-select]
  (let [-matches (atom [])
        dropdown-id (str (random-uuid))]
    (add-watch -matches :matches
               #(when-let [matches (seq @-matches)]
                  (refresh-node dropdown-id (list-dropdown matches on-select))))
    (add-watch -value :value #(reset! -matches (search-fn @-value)))
    [:div.dropdown
     [:div.dropdown-trigger (text -value)]
     [(str "div.dropdown-menu#" dropdown-id)]]))