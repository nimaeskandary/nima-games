(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.swap-chain
  (:import (org.lwjgl.vulkan VkExtent2D)))

(defprotocol SwapChain
  (start [this])
  (stop [this])
  (get-image-views [this])
  (get-num-images [this])
  (get-surface-format [this])
  (get-swap-chain-extent ^VkExtent2D [this])
  (get-vk-swap-chain ^Long [this]))
