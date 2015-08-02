(ns looper.middleware
  (:require [clojure.tools.nrepl.transport :as transport]
            [looper.core :as looper])
  (:use [clojure.tools.nrepl.middleware
         :as middleware
         :only (set-descriptor!)]
        [clojure.tools.nrepl.misc :only (uuid response-for)])
        ; [looper.core :as looper])
  (:import clojure.tools.nrepl.transport.Transport))

(use 'clojure.java.io)
(defn logr [& msg]
  (with-open [wrtr (writer "log.txt" :append true)]
    (.write wrtr (clojure.string/join " " (conj msg "\n")))))

(defn now []
  (System/currentTimeMillis))

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

(defn play [handler loop]
  (logr "Playing handler:" handler "loop:" loop)
  (logr "loop content:" @loop)
  (looper/play-repl-loop loop handler))

; (defn execute
;   "evaluates s-forms"
;   ([request] (execute request *ns*))
;   ([request user-ns]
;     (str
;       (try
;         (binding [*ns* user-ns] (eval (read-string request)))
;         (catch Exception e (.getLocalizedMessage e))))))

(def dummy-transport
  (reify Transport
    (recv [this] (logr "RECV:" this))
    (recv [this timeout] (logr "RECV-TIMEOUT:" this))
    (send [this resp] (logr "SEND:" resp))))

(def handler-ns (atom false))

(defn get-ns
  "Get the namespace from the eval handler."
  [handler session]
  (reset! handler-ns false)
  (let [msg {:transport dummy-transport
             :op "eval"
             :session session
             :code "(reset! looper.middleware/handler-ns *ns*)"
             }]
    (logr "Going to call the handler with:" msg)
    (handler msg)
    ; This is probably a horrible way to wait for the value!
    (while (not @handler-ns))
    @handler-ns))

(defn eval-in [eval-ns expr]
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

(defn looper-wrapper [handler]
  (fn [{:keys [op code transport] :as msg}]
    (if (and (= op "eval") (not= code ""))
      (let [input (read-string code)]
        (if (and (seq? input) (= 'looper (first input)))
          (looper-msg handler msg)
          (looper-eval-handler handler msg)))
      (handler msg))))

(set-descriptor! #'looper-wrapper
                 {
                  :requires #{"session"}
                  :expects #{"eval"}
                  :handles {"looper-wrapper"
                            {:doc "Loop stuff"}}})


