(ns debatesim.core
  (:require [om.core :as om :include-macros true]
            [om-tools.dom :as dom :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [goog.net.XhrIo :as xhr]))

(enable-console-print!)

(def speakers [:moderator :mulcair :harper :trudeau :may :trudeau :mulcair :harper])

(defonce app-state (atom {:text "Hello Chestnut!"
                          :curr-speaker 7
                          :harper {:name "Rt. Hon. Stephen Harper"
                                   :speaking false}
                          :trudeau {:name "Justin Trudeau"
                                    :speaking false}
                          :mulcair {:speaking true
                                    :name "Hon. Thomas Mulcair"}
                          :may {:name "Elizabeth May"
                                :speaking false}
                          :moderator {:speaking false}}))

(defn speak [text]
  (let [u (js/SpeechSynthesisUtterance. text)
        voices (.getVoices js/window.speechSynthesis)]
    (set! (.-voice u) (get voices 1))
    (.speak js/window.speechSynthesis u)))

(defn build-remover [speaker]
  (fn []
    (swap! app-state #(assoc-in % [speaker :speaking] false))))

(declare download-and-reschedule)

(defn process-and-reschedule [speaker response]
  (swap! app-state #(assoc-in % [:curr-speaker] speaker))
  (let [speaker (get speakers speaker)
        quote (-> response .-target .getResponseText)
        words (-> quote (.split " ") .-length)]
    (swap! app-state #(assoc-in % [speaker :speaking]  true))
    (swap! app-state #(assoc-in % [speaker :quote] quote))
    (speak quote)
    (.setTimeout js/window (build-remover speaker) (* words 400))
    (.setTimeout js/window download-and-reschedule (+ (* words 400) (- (* (.random js/Math) 2000) 1000)))
    ))

(defn download-and-reschedule []
  (let [next-speaker (mod (+ (:curr-speaker @app-state) 1) 8)]
    (xhr/send (str "/" (.substr (str (get speakers next-speaker)) 1))
              (partial process-and-reschedule next-speaker))))

(defcomponent speaker [data owner]
  (render [_]
          (dom/div {:class "speaker"}
                   (if (:speaking data)
                     (dom/div {:class "textbox"}
                              (dom/div {:class "textbox-name"}
                                       (dom/h2 (:name data)))
                              (dom/p (:quote data)))
                     nil)
                   )))

(defn main []
  (om/root
    (fn [app owner]
      (reify
        om/IRender
        (render [_]
          (dom/div
           (om/build speaker (:trudeau app))
           (om/build speaker (:may app))
           (om/build speaker (:mulcair app))
           (om/build speaker (:harper app))
           (if (:speaking (:moderator app))
             (dom/div {:class "moderator-textbox"}
                     (dom/h2 "Moderator")
                     (dom/p (:quote (:moderator app))))
             nil)))))
    app-state
    {:target (. js/document (getElementById "app"))})
  (download-and-reschedule))
