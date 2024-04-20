(ns nimaeskandary.vulkan-tutorial.chapter-3.eng.proto.surface)

(defprotocol Surface
  (start [this])
  (stop [this])
  (get-vk-surface [this]))
