
(defn show-keystroke []
  (print "Enter a keystroke: ")
  (flush)
  (let [cr (ConsoleReader.)
        keyint (.readCharacter cr)]
    (println (format "Got %d ('%c')!" keyint (char keyint)))))

(defn get-keystroke []
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
