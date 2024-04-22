(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.semaphore)

(defprotocol Semaphore
  (start [this])
  (stop [this])
  (get-vk-semaphore ^Long [this]))
