(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.command-buffer
  (:import (org.lwjgl.vulkan VkCommandBuffer)))

(defprotocol CommandBuffer
  (start [this])
  (stop [this])
  (begin-recording [this]
                   [this inheritance-info])
  (end-recording [this])
  (get-vk-command-buffer ^VkCommandBuffer [this])
  (reset [this]))
