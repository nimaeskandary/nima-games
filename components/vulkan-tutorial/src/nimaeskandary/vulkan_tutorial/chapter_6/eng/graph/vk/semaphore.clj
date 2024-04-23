(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.semaphore
  (:require [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.vulkan-utils
             :as vulkan-utils]
            [nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.device :as
             proto.device]
            [nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.semaphore :as
             proto.semaphore])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK12 VkDevice VkSemaphoreCreateInfo)))

(defn start
  [{:keys [device], :as this}]
  (println "starting semaphore")
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkDevice vk-device (proto.device/get-vk-device device)
          create-info (-> (VkSemaphoreCreateInfo/calloc stack)
                          .sType$Default)
          long-b (.mallocLong stack 1)
          _ (-> (VK12/vkCreateSemaphore vk-device create-info nil long-b)
                (vulkan-utils/vk-check "failed to create semaphore"))
          vk-semaphore (.get long-b 0)]
      (assoc this :vk-semaphore vk-semaphore))))

(defn stop
  [{:keys [device vk-semaphore], :as this}]
  (println "stopping semaphore")
  (let [vk-device (proto.device/get-vk-device device)]
    (VK12/vkDestroySemaphore vk-device vk-semaphore nil))
  this)

(defn get-vk-semaphore [{:keys [vk-semaphore]}] vk-semaphore)

(defrecord Semaphore [device]
  proto.semaphore/Semaphore
    (start [this] (start this))
    (stop [this] (stop this))
    (get-vk-semaphore [this] (get-vk-semaphore this)))
