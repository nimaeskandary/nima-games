(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.device
  (:require [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.vulkan-utils
             :as vulkan-utils]
            [nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.device :as
             proto.device]
            [nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.physical-device
             :as proto.physical-device]
            [nimaeskandary.vulkan-tutorial.chapter-6.utils :as utils])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan KHRPortabilitySubset
                             KHRSwapchain
                             VK12
                             VkDevice
                             VkDeviceCreateInfo
                             VkDeviceQueueCreateInfo
                             VkExtensionProperties
                             VkPhysicalDevice
                             VkPhysicalDeviceFeatures2
                             VkQueueFamilyProperties2
                             VkQueueFamilyProperties2$Buffer)))

(defn get-device-exts-set
  [physical-device]
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkPhysicalDevice vk-physical-device
          (proto.physical-device/get-vk-physical-device physical-device)
          num-exts-b (.callocInt stack 1)
          _ (VK12/vkEnumerateDeviceExtensionProperties vk-physical-device
                                                       ^String utils/nil*
                                                       num-exts-b
                                                       nil)
          num-exts (.get num-exts-b 0)
          _ (println (format "device supports %d extensions" num-exts))
          props-b (VkExtensionProperties/calloc num-exts stack)
          _ (VK12/vkEnumerateDeviceExtensionProperties vk-physical-device
                                                       ^String utils/nil*
                                                       num-exts-b
                                                       props-b)]
      (-> (for [^Integer i (range num-exts)]
            (let [^VkExtensionProperties props (.get props-b i)
                  ext-name (.extensionNameString props)]
              (println (format "supported device extension %s" ext-name))
              ext-name))
          set))))

(defn start
  [{:keys [physical-device], :as this}]
  (println "starting device")
  (with-open [stack (MemoryStack/stackPush)]
    (let [;; define required exts
          device-exts-set (get-device-exts-set physical-device)
          use-portability?
          (and (device-exts-set
                KHRPortabilitySubset/VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME)
               (= vulkan-utils/os-type:macos vulkan-utils/os-type))
          num-exts (if use-portability? 2 1)
          required-exts (.mallocPointer stack num-exts)
          _
          (do
            (.put required-exts
                  (.ASCII stack KHRSwapchain/VK_KHR_SWAPCHAIN_EXTENSION_NAME))
            (when use-portability?
              (.put
               required-exts
               (.ASCII
                stack
                KHRPortabilitySubset/VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME)))
            (.flip required-exts))
          ;; set up required features
          features (-> (VkPhysicalDeviceFeatures2/calloc stack)
                       (.sType
                        VK12/VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2))
          ;; enable all queue families
          ^VkQueueFamilyProperties2$Buffer queue-props-b
          (proto.physical-device/get-vk-queue-family-props physical-device)
          num-queue-families (.capacity queue-props-b)
          queue-creation-info-b
          (VkDeviceQueueCreateInfo/calloc num-queue-families stack)
          _ (doseq [^Integer i (range num-queue-families)]
              ;; need to set priorities, getting count of existing queues
              ;; and making a zeroed out priority list for each queue
              (let [priorities (.callocFloat stack
                                             (-> queue-props-b
                                                 ^VkQueueFamilyProperties2
                                                 (.get i)
                                                 .queueFamilyProperties
                                                 .queueCount))]
                (-> queue-creation-info-b
                    ^VkDeviceQueueCreateInfo (.get i)
                    (.sType VK12/VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                    (.queueFamilyIndex i)
                    (.pQueuePriorities priorities))))
          device-create-info (-> (VkDeviceCreateInfo/calloc stack)
                                 (.sType
                                  VK12/VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                                 (.ppEnabledExtensionNames required-exts)
                                 (.pEnabledFeatures (.features features))
                                 (.pQueueCreateInfos queue-creation-info-b))
          device-p (.mallocPointer stack 1)
          vk-physical-device (proto.physical-device/get-vk-physical-device
                              physical-device)
          _ (-> (VK12/vkCreateDevice vk-physical-device
                                     device-create-info
                                     nil
                                     device-p)
                (vulkan-utils/vk-check "failed to create device"))
          vk-device
          (VkDevice. (.get device-p 0) vk-physical-device device-create-info)]
      (assoc this :vk-device vk-device))))

(defn stop
  [{:keys [vk-device], :as this}]
  (println "stopping vulkan device")
  (VK12/vkDestroyDevice vk-device nil)
  this)

(defn get-physical-device [{:keys [physical-device]}] physical-device)

(defn get-vk-device [{:keys [vk-device]}] vk-device)

(defn wait-idle [{:keys [vk-device]}] (VK12/vkDeviceWaitIdle vk-device))

(defrecord Device [physical-device]
  proto.device/Device
    (start [this] (start this))
    (stop [this] (stop this))
    (get-physical-device [this] (get-physical-device this))
    (get-vk-device [this] (get-vk-device this))
    (wait-idle [this] (wait-idle this)))
