(ns nrepl-looper.middleware
  (:require [clojure.tools.nrepl.transport :as transport]
            [overtone.at-at :as at-at])
  (:use [clojure.tools.nrepl.middleware
         :as middleware
         :only (set-descriptor!)]
        [clojure.tools.nrepl.misc :only (uuid response-for)])
  (:import clojure.tools.nrepl.transport.Transport))

(use 'clojure.java.io)
(defn logr [& msg]
  (with-open [wrtr (writer "log.txt" :append true)]
    (.write wrtr (clojure.string/join " " (conj msg "\n")))))

; Adopted from Overtone
; Scheduled thread pool (created by at-at) which is to be used by default for
; all scheduled musical functions (players).
(defonce player-pool (at-at/mk-pool))

; Adopted from Overtone
(defn now
  "Returns the current time in ms"
  []
  (System/currentTimeMillis))

; Adopted from Overtone
(defn after-delay
  "Schedules fun to be executed after ms-delay milliseconds. Pool
  defaults to the player-pool."
  ([ms-delay fun] (after-delay ms-delay fun "Overtone delayed fn"))
  ([ms-delay fun description]
     (at-at/at (+ (now) ms-delay) fun player-pool :desc description)))

; Adopted from Overtone
(defn apply-at
  "Scheduled function appliction. Works identically to apply, except
   that it takes an additional initial argument: ms-time. If ms-time is
   in the future, function application is delayed until that time, if
   ms-time is in the past function application is immediate.

   If you wish to apply slightly before specific time rather than
   exactly at it, see apply-by.

   Can be used to implement the 'temporal recursion' pattern. This is
   where a function has a call to apply-at at its tail:

   (defn foo
     [t val]
     (println val)
     (let [next-t (+ t 200)]
       (apply-at next-t #'foo [next-t (inc val)])))

   (foo (now) 0) ;=> 0, 1, 2, 3...

   The fn foo is written in a recursive style, yet the recursion is
   scheduled for application 200ms in the future. By passing a function
   using #'foo syntax instead of the symbole foo, when later called by
   the scheduler it will lookup based on the symbol rather than using
   the instance of the function defined earlier. This allows us to
   redefine foo whilst the temporal recursion is continuing to execute.

   To stop an executing temporal recursion pattern, either redefine the
   function to not call itself, or use (stop)."
  {:arglists '([ms-time f args* argseq])}
  [#^clojure.lang.IFn ms-time f & args]
  (let [delay-time (- ms-time (now))]
    (if (<= delay-time 0)
      (after-delay 0 #(apply f (#'clojure.core/spread args)))
      (after-delay delay-time #(apply f (#'clojure.core/spread args))))))

(defn empty-loop []
  (atom {:start-time (now) :events []}))

(defonce recording-loop (atom false))

(defn start-recording [loop]
  (logr "Starting recording:" loop)
  (swap! loop assoc :start-time (now))
  (reset! recording-loop loop))

(defn stop-recording []
  (let [length (- (now) (@@recording-loop :start-time))]
    (swap! @recording-loop assoc :length length)
    (reset! recording-loop false)))

(defn my-eval [repl-eval cmd]
  (logr "my-eval" "cmd:" cmd)
  (repl-eval cmd))

(defn play-repl-event [start-time length event repl-eval]
  (let [offset (mod (event :offset) length)
        cmd (event :cmd)]
    (logr "play-repl-event start:" start-time "offset:" offset "at:" (+ start-time offset))
    (apply-at (+ start-time offset) my-eval [repl-eval cmd])))

(defn play-repl-events-at [events start-time length repl-eval]
  (logr "play-repl-events-at" start-time)
  (doseq [event events]
    (play-repl-event start-time length event repl-eval)))

(defn play-repl-loop [rloop repl-eval]
  (logr "rloop:" rloop "repl-eval:" repl-eval)
  (let [events (@rloop :events)
        length (@rloop :length)
        start-time (now)]
    (logr "play-repl-loop events:" events)
    (play-repl-events-at events start-time length repl-eval)
    (apply-at (+ start-time length) play-repl-loop [rloop repl-eval])
    nil))

(defn play [handler loop]
  (logr "Playing handler:" handler "loop:" loop)
  (logr "loop content:" @loop)
  (play-repl-loop loop handler))

(def dummy-transport
  (reify Transport
    (recv [this] (logr "RECV:" this))
    (recv [this timeout] (logr "RECV-TIMEOUT:" this))
    (send [this resp] (logr "SEND:" resp))))

(def handler-ns (atom false))

; Right now this is done via a shared var, waiting for the other thread to set
; it. I hope there is a better way, but I don't know it :)
(defn get-ns
  "Get the namespace from the eval handler."
  [handler session]
  (reset! handler-ns false)
  (let [msg {:transport dummy-transport
             :op "eval"
             :session session
             :code "(reset! nrepl-looper.middleware/handler-ns *ns*)"
             }]
    (logr "Going to call the handler with:" msg)
    (handler msg)
    ; This is probably a horrible way to wait for the value!
    (while (not @handler-ns))
    @handler-ns))

(defn eval-in
  "Evaluate an expression in the given namespace"
  [eval-ns expr]
  (binding [*ns* eval-ns] (eval expr)))

(defn looper-msg
  "Operate the looper -- record, start, stop"
  [handler {:keys [code transport session] :as msg}]
  (logr "LOOPER CMD" )
  (logr "handler ns:" (get-ns handler session))
  (logr "current ns:" (-> *ns* ns-name str))
  (let [cmd (first (rest (read-string code)))
        target-ns (get-ns handler session)]
    (case cmd
      "record" (start-recording (eval-in target-ns (first (rest (rest (read-string code))))))
      "stop-recording" (stop-recording)
      "play" (play handler (eval-in target-ns (first (rest (rest (read-string code))))))
      "nothing" ()
      (eval-in target-ns (first (rest (read-string code))))))
  (transport/send transport (response-for msg :status :done)))


(defn add-event [loop offset cmd]
  (swap! loop update-in [:events]
         conj {:offset offset :cmd cmd}))

(defn add-event-now [loop cmd]
  (logr "Adding event now"
           "cmd:" cmd
           "events:" (@loop :events))
  (add-event loop (- (now) (@loop :start-time)) cmd))

(defn looper-eval-handler
  "Possibly record evals into an active loop"
  [handler msg]
  (if @recording-loop
    ; This adds a fake transport to the message
    ; In other words -- all output gets thrown away!
    ; These are "REL" not "REPL" :)
    ; (let [msg (assoc msg :transport
    ;                  (reify Transport
    ;                    (recv [this] (logr "RECV"))
    ;                    (recv [this timeout] (logr "RECV-TIMEOUT"))
    ;                    (send [this resp] (logr "SEND:" resp))))]
    ; (let [msg {:transport dummy-transport
    ;            :op "eval"
    ;            :code (msg :code)
    ;            ; :id (uuid)
    ;            }]
    (let [msg (assoc msg :transport dummy-transport)]
      (add-event-now @recording-loop msg)))
  (logr "looper-eval-handler:" handler)
  (logr "looper-eval-handler msg:" msg)
  (handler msg))

(defn wrap-looper [handler]
  (fn [{:keys [op code transport] :as msg}]
    (if (and (= op "eval") (not= code ""))
      (let [input (read-string code)]
        (if (and (seq? input) (= 'looper (first input)))
          (looper-msg handler msg)
          (looper-eval-handler handler msg)))
      (handler msg))))

(set-descriptor!
  #'wrap-looper {:requires #{"session"}
                 :expects #{"eval"}})


