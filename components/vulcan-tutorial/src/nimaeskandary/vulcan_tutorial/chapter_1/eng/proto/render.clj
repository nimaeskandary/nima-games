(ns nimaeskandary.vulcan-tutorial.chapter-1.eng.proto.render)

(defprotocol Render
  (start [this])
  (stop [this])
  (render [this window scene]))
