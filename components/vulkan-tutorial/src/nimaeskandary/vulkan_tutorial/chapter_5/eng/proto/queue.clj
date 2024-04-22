(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.queue
  (:import (org.lwjgl.vulkan VkQueue)))

(defprotocol Queue
  (start [this])
  (stop [this])
  (get-vk-queue ^VkQueue [this])
  (get-queue-family-index ^Integer [this])
  (wait-idle [this])
  (submit [this command-buffers wait-semaphores dst-stage-masks
           signal-semaphores fence]))
