(ns nimaeskandary.vulcan-tutorial.chapter-2.eng.proto.mouse-input)

(defprotocol MouseInput
  (start [this])
  (stop [this])
  (get-current-pos [this])
  (get-displ-vec [this])
  (input [this])
  (is-left-button-pressed? [this])
  (is-right-button-pressed? [this]))
