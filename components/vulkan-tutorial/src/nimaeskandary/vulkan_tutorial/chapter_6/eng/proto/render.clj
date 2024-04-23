(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.render)

(defprotocol Render
  (start [this])
  (stop [this])
  (render [this]))
