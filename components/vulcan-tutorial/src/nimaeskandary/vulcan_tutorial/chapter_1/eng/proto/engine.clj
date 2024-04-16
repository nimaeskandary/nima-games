(ns nimaeskandary.vulcan-tutorial.chapter-1.eng.proto.engine)

(defprotocol Engine
  (start [this])
  (stop [this])
  (run [this])
  (init [this]))
