(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.swap-chain
  (:import (org.lwjgl.vulkan VkExtent2D)))

(defprotocol SwapChain
  (start [this])
  (stop [this])
  (get-image-views [this])
  (get-num-images [this])
  (get-surface-format [this])
  (get-swap-chain-extent ^VkExtent2D [this])
  (get-vk-swap-chain ^Long [this])
  (get-device [this])
  (acquire-next-image [this])
  (present-image [this queue])
  (get-current-frame [this])
  (get-sync-semaphores [this]))
