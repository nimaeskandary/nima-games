(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.sync-semaphores)

(defprotocol SyncSemaphores
  (start [this])
  (stop [this])
  (get-image-acquisition-semaphore [this])
  (get-render-complete-semaphore [this]))
