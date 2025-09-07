(ns the-emerald-green.web.prompts
  (:require [the-emerald-green.web.utils :refer [debounce default-wait-ms]]))

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

(defn textarea [-value & {:keys [wait] :or {wait default-wait-ms}}]
  [:textarea.textarea
   {:oninput (debounce #(reset! -value (-> % .-target .-value)) wait)
    :rows 10}
   @-value])
