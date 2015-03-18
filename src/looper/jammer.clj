
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

(defn jam []
  (while true
    (let [note (- (keycode->midinote (get-keystroke)) 20)]
      (sampled-piano note)
      ; (midi-note midi-output-device note 127 500)
      (print (find-note-name note)))))

