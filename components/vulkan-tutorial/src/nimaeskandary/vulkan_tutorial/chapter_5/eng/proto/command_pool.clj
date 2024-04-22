(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.command-pool)

(defprotocol CommandPool
  (start [this])
  (stop [this])
  (get-device [this])
  (get-vk-command-pool ^Long [this]))
