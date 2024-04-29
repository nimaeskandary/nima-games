(ns nimaeskandary.vulkan-tutorial.chapter-8.eng.graph.vk.fence
  (:require [nimaeskandary.vulkan-tutorial.chapter-8.eng.graph.vk.device :as
             vk.device]
            [nimaeskandary.vulkan-tutorial.chapter-8.eng.graph.vk.vulkan-utils
             :as vulkan-utils])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK12 VkDevice VkFenceCreateInfo)))

(defprotocol FenceI
  (start [this])
  (stop [this])
  (fence-wait [this])
  (get-vk-fence ^Long [this])
  (reset [this]))

(defn -start
  [{:keys [device signaled?], :as this}]
  (println "starting fence")
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkDevice vk-device (vk.device/get-vk-device device)
          create-info (-> (VkFenceCreateInfo/calloc stack)
                          .sType$Default
                          (.flags
                           (if signaled? VK12/VK_FENCE_CREATE_SIGNALED_BIT 0)))
          long-b (.mallocLong stack 1)
          _ (-> (VK12/vkCreateFence vk-device create-info nil long-b)
                (vulkan-utils/vk-check "failed to create fence"))
          vk-fence (.get long-b 0)]
      (assoc this :vk-fence vk-fence))))

(defn -stop
  [{:keys [device vk-fence], :as this}]
  (println "stopping fence")
  (let [vk-device (vk.device/get-vk-device device)]
    (VK12/vkDestroyFence vk-device vk-fence nil))
  this)

(defn -fence-wait
  [{:keys [device ^Long vk-fence]}]
  (let [vk-device (vk.device/get-vk-device device)]
    (VK12/vkWaitForFences vk-device vk-fence true Long/MAX_VALUE)))

(defn -get-vk-fence [{:keys [vk-fence]}] vk-fence)

(defn -reset
  [{:keys [device ^Long vk-fence]}]
  (let [vk-device (vk.device/get-vk-device device)]
    (VK12/vkResetFences vk-device vk-fence)))

(defrecord Fence [device signaled?]
  FenceI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (fence-wait [this] (-fence-wait this))
    (get-vk-fence [this] (-get-vk-fence this))
    (reset [this] (-reset this)))
