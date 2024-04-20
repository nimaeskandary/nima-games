(ns nimaeskandary.vulkan-tutorial.chapter-4.eng.proto.queue)

(defprotocol Queue
  (start [this])
  (stop [this])
  (get-vk-queue [this])
  (wait-idle [this]))
