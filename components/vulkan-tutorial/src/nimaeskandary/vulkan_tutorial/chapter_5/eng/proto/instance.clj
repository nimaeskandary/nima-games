(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.instance)

(defprotocol Instance
  (start [this])
  (stop [this])
  (get-vk-instance [this]))
