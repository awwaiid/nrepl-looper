(ns looper.core
  (:require
    [clojure.tools.nrepl :as repl]))

(use 'overtone.live)
(use 'overtone.inst.piano)
(use 'overtone.inst.sampled-piano)
(use 'overtone.inst.synth)

; Set up our own nrepl in our running overtone on a known port
(use '[clojure.tools.nrepl.server :only (start-server stop-server)])
(defonce server (start-server :port 7888))

; We send evals through nrepl, so that namespace and overtone
; connection stuff is all done for us
(defn repl-eval [cmd]
  (with-open [conn (repl/connect :port 7888)]
    (-> (repl/client conn 1000)
        (repl/message
          {:op "eval"
           :code (str "(do (ns looper.core) " cmd ")")})
        doall)))


; Example datastructure for a loop
; It is an atom so that it can be modified during playback
(def loop-1
  (atom
    {:length 4000 ; Longer will be mod
     :start-time 1426700231396 ; unix milliseconds
     :events [:offset 0    :cmd "piano 60"
              :offset 1000 :cmd "piano 60"
              :offset 2000 :cmd "piano 63"
              :offset 3000 :cmd "piano 65"]}))

(defn add-event [loop offset cmd]
  (swap! loop update-in [:events]
         conj {:offset offset :cmd cmd}))

(defn add-event-now [loop cmd]
  (add-event loop (- (now) (@loop :start-time)) cmd))

(defn play-repl-event [start-time length event]
  (let [offset (mod (event :offset) length)
        cmd (event :cmd)
        cmd (str "(do (println \"In eval\") " cmd ")")]
    (println "play-repl-event start:" start-time "offset:" offset "at:" (+ start-time offset) "cmd:" cmd)
    (apply-at (+ start-time offset) repl-eval [cmd])))

(defn play-repl-events-at [events start-time length]
  (println "play-repl-events-at" start-time events)
  (doseq [event events]
    (play-repl-event start-time length event)))

(defn play-repl-loop [rloop]
  (let [events (@rloop :events)
        length (@rloop :length)
        start-time (now)]
    (println "play-repl-loop" @rloop)
    (play-repl-events-at events start-time length)
    (apply-at (+ start-time length) play-repl-loop [rloop])
    nil))

(defn wrap-repl-execute [repl-execute]
  "Modify reply's execute to record the events with timing"
  (fn [options form]
    (let [loop (options :loop)
          start-time (@loop :start-time)]
      (add-event-now loop form)
      (println "form:" form)
      (println "events:" (@loop :events))
      (repl-execute options form))))

(defn rec-repl []
  (let [loop (atom {:start-time (now) :events []})]
    (reply.eval-modes.standalone/main
      {:ns 'looper.core
       :loop loop})
    (swap! loop update-in [:events] #(subvec % 1)) ; First thing is some REPL junk, remove
    (swap! loop conj {:length (- (now) (@loop :start-time))})
    loop))

