(ns nimaeskandary.vulkan-tutorial.chapter-4.eng.proto.instance)

(defprotocol Instance
  (start [this])
  (stop [this])
  (get-vk-instance [this]))
