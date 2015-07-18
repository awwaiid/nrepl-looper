(ns looper.middleware
  (:require [clojure.tools.nrepl.transport :as transport])
  (:use [clojure.tools.nrepl.middleware
         :as middleware
         :only (set-descriptor!)]
        [clojure.tools.nrepl.misc :only (response-for)]
        [looper.core :as looper])
  (:import clojure.tools.nrepl.transport.Transport))

(defn now []
  (System/currentTimeMillis))

(defn empty-loop []
  (atom {:start-time (now) :events []}))

(defonce recording-loop (atom false))

(defn start-recording [loop]
  (println "Starting recording:" loop)
  (swap! loop assoc :start-time (now))
  (reset! recording-loop loop))

(defn stop-recording []
  (let [length (- (now) (@@recording-loop :start-time))]
    (swap! @recording-loop assoc :length length)
    (reset! recording-loop false)))

(defn play [handler loop]
  (looper/play-repl-loop loop handler))

(defn looper-msg
  "Operate the looper -- record, start, stop"
  [handler {:keys [code transport] :as msg}]
  (println "LOOPER CMD")
  (read-string code)
  (println "CASE")
  (let [cmd (first (rest (read-string code)))]
    (case cmd
      "record" (start-recording (eval (first (rest (rest (read-string code))))))
      "stop-recording" (stop-recording)
      "play" (play handler (first (rest (rest (read-string code)))))
      (eval (first (rest (read-string code))))))
  (transport/send transport (response-for msg :status :done)))

(use 'clojure.java.io)
(defn log [msg]
  (with-open [wrtr (writer "log.txt" :append true)]
    (.write wrtr (clojure.string/join [msg "\n"]))))


(defn looper-eval-handler
  "Possibly record evals into an active loop"
  [handler msg]
  (if @recording-loop
    ; This adds a fake transport to the message
    ; In other words -- all output gets thrown away!
    ; These are "REL" not "REPL" :)
    (let [msg (assoc msg :transport
                     (reify Transport
                       (recv [this] (log "RECV"))
                       (recv [this timeout] (log "RECV-TIMEOUT"))
                       (send [this resp] (log (clojure.string/join ["SEND:" resp])))))
          ]
      (looper/add-event-now @recording-loop msg)))
  (println "looper-eval-handler:" handler)
  (println "looper-eval-handler msg:" msg)
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
                  ; :requires #{"session"}
                  :expects #{"eval" "session"}
                  :handles {"looper-wrapper"
                            {:doc "Loop stuff"}}})


