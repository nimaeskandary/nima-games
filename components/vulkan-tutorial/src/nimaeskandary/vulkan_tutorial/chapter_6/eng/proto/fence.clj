(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.fence)

(defprotocol Fence
  (start [this])
  (stop [this])
  (fence-wait [this])
  (get-vk-fence ^Long [this])
  (reset [this]))
