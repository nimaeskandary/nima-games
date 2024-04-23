(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.queue
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.vulkan-utils :as
     vulkan-utils]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.device :as proto.device]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.fence :as proto.fence]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.physical-device :as
     proto.physical-device]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.queue :as proto.queue]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.surface :as
     proto.surface])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan KHRSurface
                             VK12
                             VkDevice
                             VkDeviceQueueInfo2
                             VkPhysicalDevice
                             VkQueue
                             VkQueueFamilyProperties2
                             VkQueueFamilyProperties2$Buffer
                             VkSubmitInfo)))

(defn start
  [{:keys [device queue-family-index queue-index], :as this}]
  (println "starting queue")
  (with-open [stack (MemoryStack/stackPush)]
    (let [queue-p (.mallocPointer stack 1)
          ^VkDevice vk-device (proto.device/get-vk-device device)
          vk-device-queue-info
          (-> (VkDeviceQueueInfo2/calloc stack)
              (.sType (VK12/VK_STRUCTURE_TYPE_DEVICE_QUEUE_INFO_2))
              (.queueFamilyIndex queue-family-index)
              (.queueIndex queue-index))
          _ (VK12/vkGetDeviceQueue2 vk-device vk-device-queue-info queue-p)
          queue (.get queue-p 0)
          vk-queue (VkQueue. queue vk-device)]
      (assoc this :vk-queue vk-queue :queue-family-index queue-family-index))))

;; queues were pre created when the logical device was created, when the
;; logical
;; device is cleaned up the queues will also be destroyed
(defn stop [_])

(defn get-vk-queue [{:keys [vk-queue]}] vk-queue)

(defn get-queue-family-index [{:keys [queue-family-index]}] queue-family-index)

(defn wait-idle [{:keys [vk-queue]}] (VK12/vkQueueWaitIdle vk-queue))

(defn submit
  [{:keys [^VkQueue vk-queue]} command-buffers wait-semaphores dst-stage-masks
   signal-semaphores fence]
  (with-open [stack (MemoryStack/stackPush)]
    ;; todo there is a v2 of VkSubmitInfo
    (let [submit-info (-> (VkSubmitInfo/calloc stack)
                          .sType$Default
                          (.pCommandBuffers command-buffers)
                          (.pSignalSemaphores signal-semaphores))
          _ (if wait-semaphores
              (-> submit-info
                  (.waitSemaphoreCount (.capacity wait-semaphores))
                  (.pWaitSemaphores wait-semaphores)
                  (.pWaitDstStageMask dst-stage-masks))
              (.waitSemaphoreCount submit-info 0))
          ^Long fence-handle
          (if fence (proto.fence/get-vk-fence fence) VK12/VK_NULL_HANDLE)]
      (-> (VK12/vkQueueSubmit vk-queue submit-info fence-handle)
          (vulkan-utils/vk-check "failed to submit command to queue")))))

(defrecord Queue [device queue-family-index queue-index]
  proto.queue/Queue
    (start [this] (start this))
    (stop [this] (stop this))
    (get-vk-queue [this] (get-vk-queue this))
    (get-queue-family-index [this] (get-queue-family-index this))
    (wait-idle [this] (wait-idle this))
    (submit [this command-buffers wait-semaphores dst-stage-masks
             signal-semaphores fence]
      (submit this
              command-buffers
              wait-semaphores
              dst-stage-masks
              signal-semaphores
              fence)))

(defn get-graphics-queue-family-index
  [device]
  (let [physical-device (proto.device/get-physical-device device)
        ^VkQueueFamilyProperties2$Buffer queue-props-b
        (proto.physical-device/get-vk-queue-family-props physical-device)
        num-queues-families (.capacity queue-props-b)
        graphics-index (atom nil)]
    (doseq [^Integer i (range num-queues-families)
            :while (nil? @graphics-index)]
      (let [props (-> ^VkQueueFamilyProperties2 (.get queue-props-b i)
                      .queueFamilyProperties)
            is-graphics-queue?
            (not= 0 (bit-and (.queueFlags props) VK12/VK_QUEUE_GRAPHICS_BIT))]
        (when is-graphics-queue? (reset! graphics-index i))))
    (when (nil? @graphics-index)
      (throw (Exception. "failed to get graphics queue family index")))
    @graphics-index))

(defn get-presentation-queue-family-index
  [device surface]
  (with-open [stack (MemoryStack/stackPush)]
    (let [physical-device (proto.device/get-physical-device device)
          ^VkPhysicalDevice vk-physical-device
          (proto.physical-device/get-vk-physical-device physical-device)
          vk-surface (proto.surface/get-vk-surface surface)
          ^VkQueueFamilyProperties2$Buffer queue-props-b
          (proto.physical-device/get-vk-queue-family-props physical-device)
          num-queues-families (.capacity queue-props-b)
          int-b (.mallocInt stack 1)
          presentation-index (atom nil)]
      (doseq [^Integer i (range num-queues-families)
              :while (nil? @presentation-index)]
        (KHRSurface/vkGetPhysicalDeviceSurfaceSupportKHR vk-physical-device
                                                         i
                                                         vk-surface
                                                         int-b)
        (when (= VK12/VK_TRUE (.get int-b 0)) (reset! presentation-index i)))
      (when (nil? @presentation-index)
        (throw (Exception. "failed to get presentation queue family index")))
      @presentation-index)))

(defn create-graphics-queue
  [device queue-index]
  (proto.queue/start
   (->Queue device (get-graphics-queue-family-index device) queue-index)))

(defn create-presentation-queue
  [device surface queue-index]
  (proto.queue/start (->Queue device
                              (get-presentation-queue-family-index device
                                                                   surface)
                              queue-index)))
