
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
