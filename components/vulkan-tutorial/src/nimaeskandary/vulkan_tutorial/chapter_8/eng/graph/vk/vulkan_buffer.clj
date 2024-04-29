(ns nimaeskandary.vulkan-tutorial.chapter-8.eng.graph.vk.vulkan-buffer
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-8.eng.graph.vk.device :as vk.device]
    [nimaeskandary.vulkan-tutorial.chapter-8.eng.graph.vk.physical-device :as
     vk.physical-device]
    [nimaeskandary.vulkan-tutorial.chapter-8.eng.graph.vk.vulkan-utils :as
     vulkan-utils])
  (:import (org.lwjgl PointerBuffer)
           (org.lwjgl.system MemoryStack MemoryUtil)
           (org.lwjgl.vulkan VK12
                             VkBufferCreateInfo
                             VkDevice
                             VkMemoryAllocateInfo
                             VkMemoryRequirements)))

(defprotocol VulkanBufferI
  (start [this])
  (stop [this])
  (get-buffer ^Long [this])
  (get-requested-size ^Long [this])
  (map-memory ^Long [this])
  (unmap-memory [this]))

(defn -start
  [{:keys [device requested-size usage req-mask], :as this}]
  (println "starting vulkan buffer")
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkDevice vk-device (vk.device/get-vk-device device)
          vk-physical-device (vk.device/get-physical-device device)
          buffer-create-info (-> (VkBufferCreateInfo/calloc stack)
                                 .sType$Default
                                 (.size requested-size)
                                 (.usage usage)
                                 (.sharingMode VK12/VK_SHARING_MODE_EXCLUSIVE))
          long-b (.mallocLong stack 1)
          _ (-> (VK12/vkCreateBuffer vk-device buffer-create-info nil long-b)
                (vulkan-utils/vk-check "failed to create buffer"))
          buffer (.get long-b 0)
          ;; todo version 2 of mem requirements
          mem-reqs (VkMemoryRequirements/malloc stack)
          _ (VK12/vkGetBufferMemoryRequirements vk-device buffer mem-reqs)
          mem-alloc (-> (VkMemoryAllocateInfo/calloc stack)
                        .sType$Default
                        (.allocationSize (.size mem-reqs))
                        (.memoryTypeIndex
                         (vk.physical-device/memory-type-from-props
                          vk-physical-device
                          (.memoryTypeBits mem-reqs)
                          req-mask)))
          _ (-> (VK12/vkAllocateMemory vk-device mem-alloc nil long-b)
                (vulkan-utils/vk-check "failed to allocate memory"))
          memory (.get long-b 0)
          allocation-size (.allocationSize mem-alloc)
          pointer-b (MemoryUtil/memAllocPointer 1)]
      ;; todo version 2 of bind buffer memory
      (-> (VK12/vkBindBufferMemory vk-device buffer memory 0)
          (vulkan-utils/vk-check "failed to bind buffer memory"))
      (assoc this
             :allocation-size allocation-size
             :buffer buffer
             :memory memory
             :pointer-b pointer-b
             :mapped-memory-atom (atom nil)))))

(defn -stop
  [{:keys [^PointerBuffer pointer-b device buffer memory], :as this}]
  (println "stopping vulkan buffer")
  (let [vk-device (vk.device/get-vk-device device)]
    (MemoryUtil/memFree pointer-b)
    (VK12/vkDestroyBuffer vk-device buffer nil)
    (VK12/vkFreeMemory vk-device memory nil))
  this)

(defn -get-buffer [{:keys [buffer]}] buffer)

(defn -get-requested-size [{:keys [requested-size]}] requested-size)

(defn -map-memory
  [{:keys [pointer-b memory allocation-size device mapped-memory-atom]}]
  (when (not @mapped-memory-atom)
    (-> (VK12/vkMapMemory (vk.device/get-vk-device device)
                          memory
                          0 allocation-size
                          0 pointer-b)
        (vulkan-utils/vk-check "failed to mapp buffer"))
    (reset! mapped-memory-atom (.get pointer-b 0)))
  @mapped-memory-atom)

(defn -unmap-memory
  [{:keys [mapped-memory-atom device memory]}]
  (when @mapped-memory-atom
    (VK12/vkUnmapMemory (vk.device/get-vk-device device) memory)
    (reset! mapped-memory-atom nil)))

(defrecord VulkanBuffer [device requested-size usage req-mask]
  VulkanBufferI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (get-buffer [this] (-get-buffer this))
    (get-requested-size [this] (-get-requested-size this))
    (map-memory [this] (-map-memory this))
    (unmap-memory [this] (-unmap-memory this)))
