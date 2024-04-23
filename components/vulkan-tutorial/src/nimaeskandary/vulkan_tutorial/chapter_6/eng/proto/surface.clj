(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.surface)

(defprotocol Surface
  (start [this])
  (stop [this])
  (get-vk-surface ^Long [this]))
