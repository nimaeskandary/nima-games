(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.physical-device
  (:import (org.lwjgl.vulkan VkPhysicalDevice VkQueueFamilyProperties2$Buffer)))

(defprotocol PhysicalDevice
  (start [this])
  (stop [this])
  (get-device-name ^String [this])
  (get-vk-mem-props [this])
  (get-vk-physical-device ^VkPhysicalDevice [this])
  (get-vk-physical-device-features [this])
  (get-vk-physical-device-props [this])
  (get-vk-queue-family-props ^VkQueueFamilyProperties2$Buffer [this]))
