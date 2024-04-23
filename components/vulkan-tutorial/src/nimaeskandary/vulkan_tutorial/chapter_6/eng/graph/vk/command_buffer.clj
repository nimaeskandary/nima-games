(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.command-buffer
  (:require [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.command-pool
             :as vk.command-pool]
            [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.device :as
             vk.device]
            [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.vulkan-utils
             :as vulkan-utils])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK12
                             VkCommandBuffer
                             VkCommandBufferAllocateInfo
                             VkCommandBufferBeginInfo
                             VkCommandBufferInheritanceInfo
                             VkDevice)))

(defprotocol CommandBufferI
  (start [this])
  (stop [this])
  (begin-recording [this]
                   [this inheritance-info])
  (end-recording [this])
  (get-vk-command-buffer ^VkCommandBuffer [this])
  (reset [this]))

(defrecord InheritanceInfo [^Long vk-render-pass ^Long vk-frame-buffer
                            ^Integer sub-pass])

(defn -start
  [{:keys [command-pool primary?], :as this}]
  (println "starting command buffer")
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkDevice vk-device (-> (vk.command-pool/get-device command-pool)
                                  vk.device/get-vk-device)
          vk-command-pool (vk.command-pool/get-vk-command-pool command-pool)
          allocate-info (-> (VkCommandBufferAllocateInfo/calloc stack)
                            .sType$Default
                            (.commandPool vk-command-pool)
                            (.level (if primary?
                                      VK12/VK_COMMAND_BUFFER_LEVEL_PRIMARY
                                      VK12/VK_COMMAND_BUFFER_LEVEL_SECONDARY))
                            (.commandBufferCount 1))
          pointer-b (.mallocPointer stack 1)
          _ (->
              (VK12/vkAllocateCommandBuffers vk-device allocate-info pointer-b)
              (vulkan-utils/vk-check
               "failed to allocate render command buffer"))
          vk-command-buffer (VkCommandBuffer. (.get pointer-b 0) vk-device)]
      (assoc this :vk-command-buffer vk-command-buffer))))

(defn -stop
  [{:keys [command-pool ^VkCommandBuffer vk-command-buffer], :as this}]
  (println "stopping command buffer")
  (let [vk-device (-> (vk.command-pool/get-device command-pool)
                      vk.device/get-vk-device)
        ^Long vk-command-pool (vk.command-pool/get-vk-command-pool
                               command-pool)]
    (VK12/vkFreeCommandBuffers vk-device vk-command-pool vk-command-buffer))
  this)

(defn -begin-recording
  ([this] (begin-recording this nil))
  ([{:keys [vk-command-buffer primary? one-time-submit?]}
    {:keys [vk-render-pass vk-frame-buffer sub-pass], :as inheritance-info}]
   (with-open [stack (MemoryStack/stackPush)]
     (let [command-buff-info (-> (VkCommandBufferBeginInfo/calloc stack)
                                 .sType$Default)]
       (when one-time-submit?
         (.flags command-buff-info
                 VK12/VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT))
       (when (not primary?)
         (when (nil? inheritance-info)
           (throw (Exception.
                   "secondary buffers must declare inheritance info")))
         (.pInheritanceInfo command-buff-info
                            (-> (VkCommandBufferInheritanceInfo/calloc stack)
                                .sType$Default
                                (.renderPass vk-render-pass)
                                (.subpass sub-pass)
                                (.framebuffer vk-frame-buffer)))
         (.flags command-buff-info
                 VK12/VK_COMMAND_BUFFER_USAGE_RENDER_PASS_CONTINUE_BIT))
       (-> (VK12/vkBeginCommandBuffer vk-command-buffer command-buff-info)
           (vulkan-utils/vk-check "failed to begin command buffer"))))))

(defn -end-recording
  [{:keys [vk-command-buffer]}]
  (-> (VK12/vkEndCommandBuffer vk-command-buffer)
      (vulkan-utils/vk-check "failed to end command buffer")))

(defn -get-vk-command-buffer [{:keys [vk-command-buffer]}] vk-command-buffer)

(defn -reset
  [{:keys [vk-command-buffer]}]
  (VK12/vkResetCommandBuffer
   vk-command-buffer
   VK12/VK_COMMAND_BUFFER_RESET_RELEASE_RESOURCES_BIT))

(defrecord CommandBuffer [command-pool primary? one-time-submit?]
  CommandBufferI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (begin-recording [this] (-begin-recording this))
    (begin-recording [this inheritance-info]
      (-begin-recording this inheritance-info))
    (end-recording [this] (-end-recording this))
    (get-vk-command-buffer [this] (-get-vk-command-buffer this))
    (reset [this] (-reset this)))
