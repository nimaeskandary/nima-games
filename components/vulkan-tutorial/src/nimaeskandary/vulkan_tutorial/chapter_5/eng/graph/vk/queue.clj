(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.queue
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.device :as proto.device]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.physical-device :as
     proto.physical-device]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.queue :as proto.queue])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK12
                             VkDevice
                             VkDeviceQueueInfo2
                             VkQueue
                             VkQueueFamilyProperties2
                             VkQueueFamilyProperties2$Buffer)))

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
      (assoc this :vk-queue vk-queue))))

;; queues were pre created when the logical device was created, when the
;; logical
;; device is cleaned up the queues will also be destroyed
(defn stop [_])

(defn get-vk-queue [{:keys [vk-queue]}] vk-queue)

(defn wait-idle [{:keys [vk-queue]}] (VK12/vkQueueWaitIdle vk-queue))

(defrecord Queue [device queue-family-index queue-index]
  proto.queue/Queue
    (start [this] (start this))
    (stop [this] (stop this))
    (get-vk-queue [this] (get-vk-queue this))
    (wait-idle [this] (wait-idle this)))

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

(defn create-graphics-queue
  [device queue-index]
  (proto.queue/start
   (->Queue device (get-graphics-queue-family-index device) queue-index)))
