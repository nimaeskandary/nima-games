(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.command-pool
  (:require [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.vulkan-utils
             :as vulkan-utils]
            [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.device :as
             proto.device]
            [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.command-pool :as
             proto.command-pool])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK12 VkCommandPoolCreateInfo VkDevice)))

(defn start
  [{:keys [device queue-family-index], :as this}]
  (println "starting command pool")
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkDevice vk-device (proto.device/get-vk-device device)
          pool-info (-> (VkCommandPoolCreateInfo/calloc stack)
                        .sType$Default
                        (.flags
                         VK12/VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                        (.queueFamilyIndex queue-family-index))
          long-b (.mallocLong stack 1)
          _ (-> (VK12/vkCreateCommandPool vk-device pool-info nil long-b)
                (vulkan-utils/vk-check "failed to create command pool"))
          vk-command-pool (.get long-b 0)]
      (assoc this :vk-command-pool vk-command-pool))))

(defn stop
  [{:keys [device vk-command-pool], :as this}]
  (println "stopping command pool")
  (let [vk-device (proto.device/get-vk-device device)]
    (VK12/vkDestroyCommandPool vk-device vk-command-pool nil))
  this)

(defn get-device [{:keys [device]}] device)

(defn get-vk-command-pool [{:keys [vk-command-pool]}] vk-command-pool)

(defrecord CommandPool [device queue-family-index]
  proto.command-pool/CommandPool
    (start [this] (start this))
    (stop [this] (stop this))
    (get-device [this] (get-device this))
    (get-vk-command-pool [this] (get-vk-command-pool this)))
