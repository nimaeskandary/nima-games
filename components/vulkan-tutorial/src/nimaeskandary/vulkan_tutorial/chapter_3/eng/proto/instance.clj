(ns nimaeskandary.vulkan-tutorial.chapter-3.eng.proto.instance)

(defprotocol Instance
  (start [this])
  (stop [this])
  (get-vk-instance [this]))
