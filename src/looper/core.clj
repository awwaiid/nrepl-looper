(ns looper.core
  ;  (:refer-clojure :exclude [map reduce into partition partition-by take merge])
  (:import [jline.console ConsoleReader])
  (:require
    [clojure.core.async
     :as async
     :refer [chan timeout <! <!! >! >!! alt! alt!! alts! go go-loop]]))

(use 'overtone.live)
(use 'overtone.inst.piano)
(use 'overtone.inst.sampled-piano)
(use 'overtone.inst.synth)

(defn show-keystroke []
  (print "Enter a keystroke: ")
  (flush)
  (let [cr (ConsoleReader.)
        keyint (.readCharacter cr)]
    (println (format "Got %d ('%c')!" keyint (char keyint)))))

(defn get-keystroke []
  ;  (print "Enter a keystroke: ")
  (flush)
  (let [cr (ConsoleReader.)]
    (.readCharacter cr)))

(defn add-trigger [triggers code]
  (let [keycode (get-keystroke)]
    (assoc triggers keycode code)))

(defn do-trigger [triggers keycode]
  (eval (triggers keycode)))

(defn trigger-play-loop [triggers]
  (let [keycode (get-keystroke)]
    (case keycode
      9 (print "TAB")
      (do-trigger triggers keycode))
    (recur triggers)))

(def loop-1 {
             :bpm 100
             :length 4
             :events {
                      0   #(piano 60)
                      0.5 #(piano 60)
                      1   #(piano 63)
                      2   #(piano 65)
                      3   #(piano 67)
                      }
             })

(defn play-event-at [metro base-time [offset event]]
  (apply-at (metro (+ base-time offset)) event [])
  )

(defn play-loop-once [metro start-beat loop]
  (doseq [event (loop :events)]
    (play-event-at metro start-beat event)
    )
  )

(defn play-loop-loop [metro start-beat loop]
  (play-loop-once metro start-beat loop)
  (let [next-loop-time (+ start-beat (loop :length))]
    (apply-at (metro next-loop-time) play-loop-loop [metro next-loop-time loop])
    )
  )

(defn play-loop [loop]
  (let [metro (metronome (loop :bpm))]
    (play-loop-loop metro 0 loop)))


(defn rec-play-loop-loop [metro start-beat loop input]
  (go []
      (let [
            [new-stuff _] (alts! [input (timeout 0)])
            loopx (if (nil? new-stuff)
                    loop
                    (assoc-in loop [:events (first new-stuff)] (second new-stuff)))]
        ;  (print "New-stuff: " new-stuff "\n")
        ;  (flush)
        ;  (print "Loop: " loopx "\n\n")
        ;  (flush)
        (play-loop-once metro start-beat loopx)
        (let [next-loop-time (+ start-beat (loopx :length))]
          (apply-at (metro next-loop-time) rec-play-loop-loop [metro next-loop-time loopx input])))))

(defn create-recorder [metro input]
  #( >!! input [metro %] ))

(defn rec-play-loop [loop input]
  (let [metro (metronome (loop :bpm))]
    (rec-play-loop-loop metro 0 loop input)
    (create-recorder metro input)))

;  (defn record-loop []
;  (print "Enter key to start loop (same key to stop)")
;  (let [stop-key (get-keystroke)]
;  (

;;; From jammer!

(def alphabet-keycodes (zipmap 
                         ; (range 65 91) 
                         (range 97 123) 
                         "abcdefghijklmnopqrstuvwxyz"))

(def numeric-keycodes {49 \1
                       50 \2 
                       51 \3
                       52 \4
                       53 \5 
                       54 \6
                       55 \7
                       56 \8
                       57 \9
                       48 \0
                       44 \,
                       46 \.
                       })

(defn keycode->char  
  "'leaky' keycode->char converter
  gives 65 -> 'a'
  ie when given a code not a char, it just send the keycode through"
  [keycode]
  (get 
    (merge alphabet-keycodes numeric-keycodes)     
    keycode keycode))

(def char->note "
  used to convert char a to midi note 54 (F#4) etc...

                the characters below should resemble of a keyboard, with the key left of z and the two right of m specified with keycode instead of a clojure char

                please note that the midi note numbers always increases +2, and that there's an odd (5,7) offset per row, according to the Wicki-Hayden note layout:

                +fourth  +oct  +fifth
                ^     ^     _
                |     |     /|
                |_ ****** /
                *      *
                *        *
                *   C    *  -> +2
                *        *
                *      *
                ******       
                "
  {\1 64 \2 66 \3 68 \4 70 \5 72 \6 74 \7 76 \8 78 \9 80 \0 82
   \q 59 \w 61 \e 63 \r 65 \t 67 \y 69 \u 71 \i 73 \o 75 \p 77
   \a 54 \s 56 \d 58 \f 60 \g 62 \h 64 \j 66 \k 68 \l 70 
   60 47 \z 49 \x 51 \c 53 \v 55 \b 57 \n 59 \m 61 \, 63 \. 65} )

(defn keycode->midinote 
  [keycode] 
  {:test (fn [] ( = (keycode->midinote 65 ) 54))}
  "gets a keycode and output a suitable MIDI-note"
  (-> keycode 
      keycode->char
      char->note))

(def midi-input-device
  (first
    (filter
      #(= (:name %) "VirMIDI [hw:2,0,0]")
      (midi-connected-receivers))))

(def midi-output-device
  (first
    (filter
      #(= (:name %) "VirMIDI [hw:2,0,0]")
      (midi-connected-receivers))))

(defn jam []
  (while true 
    (let [note (- (keycode->midinote (get-keystroke)) 20)]
      (sampled-piano note)
      (midi-note midi-output-device note 127 500)
      (print (find-note-name note))
      )
    )
  )


;  (def c (chan))

;  (go (loop []
;  (print
;  (alts! [c (timeout 5000)]))
;  (flush)
;  (recur)))


;  (async/go-loop [] (print (async/<! c)))


