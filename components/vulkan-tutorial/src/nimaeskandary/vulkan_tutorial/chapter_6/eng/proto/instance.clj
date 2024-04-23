(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.instance)

(defprotocol Instance
  (start [this])
  (stop [this])
  (get-vk-instance [this]))
