(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.engine)

(defprotocol Engine
  (start [this])
  (stop [this])
  (run [this])
  (init [this]))
