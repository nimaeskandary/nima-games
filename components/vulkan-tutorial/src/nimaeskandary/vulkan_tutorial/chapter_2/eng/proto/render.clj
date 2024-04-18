(ns nimaeskandary.vulkan-tutorial.chapter-2.eng.proto.render)

(defprotocol Render
  (start [this])
  (stop [this])
  (render [this window scene]))
