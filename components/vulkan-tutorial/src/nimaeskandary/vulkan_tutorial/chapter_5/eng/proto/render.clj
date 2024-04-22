(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.render)

(defprotocol Render
  (start [this])
  (stop [this])
  (render [this]))
