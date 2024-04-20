(ns nimaeskandary.vulkan-tutorial.chapter-3.eng.proto.device
  (:import (org.lwjgl.vulkan VkDevice)))

(defprotocol Device
  (start [this])
  (stop [this])
  (get-physical-device [this])
  (get-vk-device ^VkDevice [this])
  (wait-idle [this]))
