(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.physical-device
  (:require [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.vulkan-utils
             :as vulkan-utils]
            [nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.instance :as
             proto.instance]
            [nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.physical-device
             :as proto.physical-device]
            [nimaeskandary.vulkan-tutorial.chapter-6.utils :as utils])
  (:import (java.util.function Consumer)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan KHRSwapchain
                             VK12
                             VkExtensionProperties
                             VkInstance
                             VkPhysicalDevice
                             VkPhysicalDeviceFeatures2
                             VkPhysicalDeviceMemoryProperties2
                             VkPhysicalDeviceProperties2
                             VkQueueFamilyProperties2
                             VkQueueFamilyProperties2$Buffer)))

(defn start
  [{:keys [^VkPhysicalDevice vk-physical-device], :as this}]
  (println "starting physical device")
  (with-open [stack (MemoryStack/stackPush)]
    (let [int-b (.mallocInt stack 1)
          ;; get device properties
          vk-physical-device-props
          (-> (VkPhysicalDeviceProperties2/calloc)
              (.sType VK12/VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2))
          _ (VK12/vkGetPhysicalDeviceProperties2 vk-physical-device
                                                 vk-physical-device-props)
          ;; get device extensions
          _ (vulkan-utils/vk-check
             (VK12/vkEnumerateDeviceExtensionProperties vk-physical-device
                                                        ^String utils/nil*
                                                        int-b
                                                        nil)
             "failed to get number of device extension properties")
          vk-device-exts (VkExtensionProperties/calloc (.get int-b 0))
          _ (vulkan-utils/vk-check (VK12/vkEnumerateDeviceExtensionProperties
                                    vk-physical-device
                                    ^String utils/nil*
                                    int-b
                                    vk-device-exts)
                                   "failed to get extension properties")
          ;; get queue family props
          _ (VK12/vkGetPhysicalDeviceQueueFamilyProperties2 vk-physical-device
                                                            int-b
                                                            nil)
          vk-queue-family-props (VkQueueFamilyProperties2/calloc (.get int-b 0))
          _ (-> (.iterator vk-queue-family-props)
                (.forEachRemaining
                 (reify
                  Consumer
                    (accept [_ qfp]
                      (.sType
                       ^VkQueueFamilyProperties2 qfp
                       VK12/VK_STRUCTURE_TYPE_QUEUE_FAMILY_PROPERTIES_2)))))
          _ (VK12/vkGetPhysicalDeviceQueueFamilyProperties2
             vk-physical-device
             int-b
             vk-queue-family-props)
          ;; get features
          vk-physical-device-features
          (-> (VkPhysicalDeviceFeatures2/calloc)
              (.sType VK12/VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2))
          _ (VK12/vkGetPhysicalDeviceFeatures2 vk-physical-device
                                               vk-physical-device-features)
          vk-mem-props
          (-> (VkPhysicalDeviceMemoryProperties2/calloc)
              (.sType
               VK12/VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_MEMORY_PROPERTIES_2))
          _ (VK12/vkGetPhysicalDeviceMemoryProperties2 vk-physical-device
                                                       vk-mem-props)]
      (assoc this
             :vk-physical-device-props vk-physical-device-props
             :vk-device-exts vk-device-exts
             :vk-queue-family-props vk-queue-family-props
             :vk-physical-device-features vk-physical-device-features
             :vk-mem-props vk-mem-props))))

(defn stop
  [{:keys [vk-mem-props vk-physical-device-features vk-queue-family-props
           vk-device-exts vk-physical-device-props],
    :as this}]
  (println (format "stopping physical device %s"
                   (proto.physical-device/get-device-name this)))
  (.free vk-mem-props)
  (.free vk-physical-device-features)
  (.free vk-queue-family-props)
  (.free vk-device-exts)
  (.free vk-physical-device-props)
  this)

(defn get-device-name
  [{:keys [^VkPhysicalDeviceProperties2 vk-physical-device-props]}]
  (-> (.properties vk-physical-device-props)
      .deviceNameString))

(defn get-vk-mem-props [{:keys [vk-mem-props]}] vk-mem-props)

(defn get-vk-physical-device [{:keys [vk-physical-device]}] vk-physical-device)

(defn get-vk-physical-device-features
  [{:keys [vk-physical-device-features]}]
  vk-physical-device-features)

(defn get-vk-physical-device-props
  [{:keys [vk-physical-device-props]}]
  vk-physical-device-props)

(defn get-vk-queue-family-props
  ^VkQueueFamilyProperties2$Buffer [{:keys [vk-queue-family-props]}]
  vk-queue-family-props)

(defn has-graphics-queue-family?
  [{:keys [^VkQueueFamilyProperties2$Buffer vk-queue-family-props]}]
  (let [num-queue-families (when vk-queue-family-props
                             (.capacity vk-queue-family-props))]
    (when (and num-queue-families (pos? num-queue-families))
      (->> (range num-queue-families)
           (filter (fn [^Integer i]
                     (let [^VkQueueFamilyProperties2 family-props
                           (.get vk-queue-family-props i)
                           has-graphics-bit?
                           (not= 0
                                 (bit-and (-> (.queueFamilyProperties
                                               family-props)
                                              (.queueFlags))
                                          VK12/VK_QUEUE_GRAPHICS_BIT))]
                       has-graphics-bit?)))
           first))))

(defn has-khr-swap-chain-extension?
  [{:keys [vk-device-exts]}]
  (let [num-exts (when vk-device-exts (.capacity vk-device-exts))]
    (when (and num-exts (pos? num-exts))
      (->> (range num-exts)
           (filter (fn [i]
                     (let [ext-name (-> (.get vk-device-exts i)
                                        .extensionNameString)]
                       (= KHRSwapchain/VK_KHR_SWAPCHAIN_EXTENSION_NAME
                          ext-name))))
           first))))


(defrecord PhysicalDevice [vk-physical-device]
  proto.physical-device/PhysicalDevice
    (start [this] (start this))
    (stop [this] (stop this))
    (get-device-name [this] (get-device-name this))
    (get-vk-mem-props [this] (get-vk-mem-props this))
    (get-vk-physical-device [this] (get-vk-physical-device this))
    (get-vk-physical-device-features [this]
      (get-vk-physical-device-features this))
    (get-vk-physical-device-props [this] (get-vk-physical-device-props this))
    (get-vk-queue-family-props [this] (get-vk-queue-family-props this)))

(defn get-physical-devices
  [instance stack]
  (let [int-b (.mallocInt stack 1)
        ^VkInstance vk-instance (proto.instance/get-vk-instance instance)
        _ (-> vk-instance
              (VK12/vkEnumeratePhysicalDevices int-b nil)
              (vulkan-utils/vk-check
               "failed to get number of physical devices"))
        num-devices (.get int-b 0)
        _ (println (format "detected %d physical devices" num-devices))
        physical-devices (.mallocPointer stack num-devices)
        _ (-> vk-instance
              (VK12/vkEnumeratePhysicalDevices int-b physical-devices)
              (vulkan-utils/vk-check "failed to get physical devices"))]
    physical-devices))

(defn create-physical-device
  [instance preferred-device-name]
  (println "selecting physical devices")
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkInstance vk-instance (proto.instance/get-vk-instance instance)
          p-physical-devices (get-physical-devices instance stack)
          num-devices (when p-physical-devices (.capacity p-physical-devices))
          _ (when (or (not num-devices) (not (pos? num-devices)))
              (throw (Exception. "no physical devices found")))
          preferred-match (atom nil)
          other-valid-devices
          (doall
           (for [i (range num-devices)
                 ;; go until we find the preferred device
                 :while (nil? @preferred-match)]
             (let [vk-physical-device
                   (VkPhysicalDevice. (.get p-physical-devices i) vk-instance)
                   physical-device (proto.physical-device/start
                                    (->PhysicalDevice vk-physical-device))
                   device-name (proto.physical-device/get-device-name
                                physical-device)]
               (if (and (has-graphics-queue-family? physical-device)
                        (has-khr-swap-chain-extension? physical-device))
                 (do (println (format "device %s supports required extensions"
                                      device-name))
                     (if (and preferred-device-name
                              (= preferred-device-name device-name))
                       (do (reset! preferred-match physical-device) nil)
                       ;; collect other valid devices in case we don't find
                       ;; preferred
                       physical-device))
                 (do (println (format
                               "device %s does not support required extensions"
                               device-name))
                     (proto.physical-device/stop physical-device))))))
          ;; if the preferred device is not found arbitrarily pick the
          ;; first, could be better and pick the best available one
          selected-physical-device (or @preferred-match
                                       (first other-valid-devices))
          _ (doseq [to-clean (if @preferred-match
                               other-valid-devices
                               (rest other-valid-devices))]
              (when to-clean (proto.physical-device/stop to-clean)))]
      (when (not selected-physical-device)
        (throw (Exception. "no valid physical devices found")))
      (println (format "selected physical device %s"
                       (proto.physical-device/get-device-name
                        selected-physical-device)))
      selected-physical-device)))
