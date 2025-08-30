(ns the-emerald-green.web.prompts)

(defn text [-value & {:keys [on-submit on-change placeholder]}]
  (let [oninput
        (fn [event]
          (.preventDefault event)
          (reset! -value (-> event .-target .-value)))
        onkeydown
        (fn [event]
          (cond
            (and on-submit (= 13 (.-which event))) (on-submit @-value)
            on-change (on-change @-value)))]
    [:input.input
     (cond->
      {:type "text"
       :value @-value
       :oninput oninput}
       placeholder (assoc :placeholder placeholder)
       (or on-submit on-change) (assoc :onkeydown onkeydown))]))

(defn textarea [-value]
  [:textarea.textarea
   {:oninput #(reset! -value (-> % .-target .-value))
    :rows 10}
   @-value])
