(ns friender.core-test
  (:require [cljs.test :refer-macros [is testing]]
            [devcards.core :refer-macros [deftest]]
            [friender.core :as app]
            [reagent-query.core :as rq]
            [axiom-cljs.core :as ax]))

(deftest edit-profile-1
  ;; The edit-profile component takes a profile record, and creates
  ;; two :input boxes, one for the name and one for the tagline
  (let [record (atom {:name "Foo"
                      :tagline "foo is best"})]
    (swap! record assoc :swap! (partial swap! record))
    (let [ui (app/edit-profile @record)]
      (is (= (rq/find ui :input:value) ["Foo" "foo is best"]))
      ;; They are editable
      (let [[change-name change-tagline] (rq/find ui :input:on-change)]
        (change-name (rq/mock-change-event "Bar"))
        (is (= (:name @record) "Bar"))
        (change-tagline (rq/mock-change-event "bar is better"))
        (is (= (:tagline @record) "bar is better"))))))

(deftest create-status-1
  ;; The create-status component contains an input box, a checkbox and
  ;; a button. Clicking the button posts a new status. create-status
  ;; returns a UI function, so it can hold the contents of the input
  ;; box and the checkbox as closure atoms. The button creates a new
  ;; status with the values from the inputs.
  (let [host (-> (ax/mock-connection "foo")
                 (assoc :time (constantly 12345)))
        create-status (app/create-status host)
        uifn #(create-status host)]
    (is (= (rq/find (uifn) :input.status:value) [""]))
    (is (= (rq/find (uifn) :input.private:type) ["checkbox"]))
    (is (= (rq/find (uifn) :input.private:checked) [false]))
    ;; Editing works
    (let [[change] (rq/find (uifn) :input.status:on-change)]
      (change (rq/mock-change-event "abc")))
    (is (= (rq/find (uifn) :input.status:value) ["abc"]))
    (let [[change] (rq/find (uifn) :input.private:on-change)]
      (change (rq/mock-change-event true "checked")))
    (is (= (rq/find (uifn) :input.private:checked) [true]))
    ;; There's also a "Post" :button
    (is (= (rq/find (uifn) :button) ["Post"]))
    ;; Clicking the :button creates a new status fact
    (let [[click] (rq/find (uifn) :button:on-click)]
      (click))
    ;; This should post the status in the statuses view
    (let [[{:keys [u status private ts]}] (app/statuses host "foo")]
      (is (= u "foo"))
      (is (= status "abc"))
      (is (= private true))
      (is (= ts 12345)))
    ;; After posting, the state should be inputs should be cleared.
    (is (= (rq/find (uifn) :input.status:value) [""]))
    (is (= (rq/find (uifn) :input.private:checked) [false]))))

(deftest timeline-pane-1
  ;; The timeline-pane component displays a user's timeline. A
  ;; timeline consists of timeline entries, each consisting of a
  ;; timestamp and a term (vector) beginning with some keyword, and
  ;; some arguments. Entries are sorted by the timestamp (descending),
  ;; and each kind of term is displayed differently using the
  ;; disp-timeline-entry multimethod.
  (let [host (ax/mock-connection "foo")
        tl-mock (ax/query-mock host :friender/timeline)]
    ;; Calling the timeline-pane should query for the user's timeline.
    (app/timeline-pane host)
    ;; We respond with some content
    (tl-mock ["foo"] [1000 [:foo 1 2 3]])
    (tl-mock ["foo"] [2000 [:bar 2 3 4]])
    ;; Now the UI should include these two entries.
    (let [ui (app/timeline-pane host)]
      ;; The timestamps are the keys. They are given in descending
      ;; order.
      (is (= (rq/find ui :.timeline-entry:key) [2000 1000]))
      ;; Each entry contains a call to disp-timeline-entry with the content of the entry
      (is (= (rq/find ui {:elem app/disp-timeline-entry}) [[:bar 2 3 4] [:foo 1 2 3]])))))

(deftest home-page-1
  ;; A home-page allows a user to:
  ;; (1) edit his or her profile,
  ;; (2) write statuses, and
  ;; (3) view timeline, which combines both statuses and notifications
  (let [host (ax/mock-connection "foo")
        ui (app/home-page host)]
    ;; Initially, there should be a :button for creating a profile
    (is (= (rq/find ui :button.create-profile) ["Create Profile"]))
    ;; Pressing the button removes it from the UI, and shows an edit-profile control
    (let [[click] (rq/find ui :button.create-profile:on-click)]
      (click)
      (let [ui (app/home-page host)]
        ;; The button is gone
        (is (= (rq/find ui :button.create-profile) []))
        ;; There is an edit-profile component in its place
        (is (= (first (first (rq/find ui :div.profile))) app/edit-profile))))
    ;; Should contain a create-status component
    (is (= (rq/find ui :.create-status) [[app/create-status host]]))
    ;; Should also contain the user's timeline
    (is (= (rq/find ui :.timeline) [[app/timeline-pane host]]))))
