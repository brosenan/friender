(ns friender.core
  (:refer-clojure :exclude [uuid?])
  (:require [reagent.core :as r]
            [axiom-cljs.core :as ax]
            [declroute.core :as dr])
  (:require-macros [axiom-cljs.macros :refer [defview defquery user]]))

;; Remove this before going to production...
(enable-console-print!)

(defn input [{:keys [swap!] :as record} field]
  [:input {:value (record field)
           :on-change #(swap! assoc field (.-target.value %))}])

(defn edit-profile [{:keys [name tagline swap!] :as record}]
  [:div
   (input record :name)
   (input record :tagline)])


(defview statuses [u]
  [:friender/status u status ts]
  :store-in (r/atom nil))

(defn create-status [host]
  (let [status (r/atom "")]
    (fn [host]
      (let [{:keys [add]} (meta (statuses host (user host)))]
        [:div
         [:input {:value @status
                  :on-change #(reset! status (.-target.value %))}]
         [:button {:on-click #(add {:status @status
                                    :ts ((:time host))})} "Post"]]))))

(defn disp-timeline-entry [])

(defquery timeline [u]
  [:friender/timeline u -> ts entry]
  :store-in (r/atom nil)
  :order-by (- ts))

(defn timeline-pane [host]
  (let [entries (timeline host (user host))]
    (for [{:keys [ts entry]} entries]
      [:div.timeline-entry {:key ts}
       [disp-timeline-entry entry]])))

(defview user-profile [u]
  [:friender/profile u name tagline]
  :store-in (r/atom nil))

(defn user-page [host]
  (let [profiles (user-profile host (user host))]
    [:div
     (cond (empty? profiles)
           [:button.create-profile {:on-click #((-> profiles meta :add) {})} "Create Profile"]
           :else
           [:div.profile [edit-profile (first profiles)]])
     [:div.create-status [create-status host]]]))

(def page (r/atom nil))
(dr/watch-uri page)

(let [elem (js/document.getElementById "app")]
  (when elem
    (r/render @page elem)))


