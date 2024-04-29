(ns nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.image
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.device :as vk.device]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.physical-device :as
     vk.physical-device]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.vulkan-utils :as
     vulkan-utils])
  (:import (java.util.function Consumer)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK12
                             VkDevice
                             VkExtent3D
                             VkImageCreateInfo
                             VkMemoryAllocateInfo
                             VkMemoryRequirements)))

(defprotocol ImageI
  (start [this])
  (stop [this])
  (get-format [this])
  (get-mip-levels [this])
  (get-vk-image [this])
  (get-vk-memory [this]))

(defn- -start
  [{:keys [device],
    {:keys [array-layers format height mip-levels sample-count usage width]}
    :image-data,
    :as this}]
  (println "starting image")
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkDevice vk-device (vk.device/get-vk-device device)
          image-create-info (-> (VkImageCreateInfo/calloc stack)
                                .sType$Default
                                (.imageType VK12/VK_IMAGE_TYPE_2D)
                                (.format format)
                                (.extent (reify
                                          Consumer
                                            (accept [_ it]
                                              (-> ^VkExtent3D it
                                                  (.width width)
                                                  (.height height)
                                                  (.depth 1)))))
                                (.mipLevels mip-levels)
                                (.arrayLayers array-layers)
                                (.samples sample-count)
                                (.initialLayout VK12/VK_IMAGE_LAYOUT_UNDEFINED)
                                (.sharingMode VK12/VK_SHARING_MODE_EXCLUSIVE)
                                (.tiling VK12/VK_IMAGE_TILING_OPTIMAL)
                                (.usage usage))
          long-p (.mallocLong stack 1)
          _ (-> (VK12/vkCreateImage vk-device image-create-info nil long-p)
                (vulkan-utils/vk-check "failed to create image"))
          vk-image (.get long-p 0)
          ;; get memory requirements for this object
          ;; todo v2 of vkmemoryrequirements
          mem-reqs (VkMemoryRequirements/calloc stack)
          _ (VK12/vkGetImageMemoryRequirements vk-device vk-image mem-reqs)
          ;; select memory size and type
          mem-alloc (-> (VkMemoryAllocateInfo/calloc stack)
                        .sType$Default
                        (.allocationSize (.size mem-reqs))
                        (.memoryTypeIndex
                         (-> (vk.device/get-physical-device device)
                             (vk.physical-device/memory-type-from-props
                              (.memoryTypeBits mem-reqs)
                              0))))
          ;; allocate memory
          _ (-> (VK12/vkAllocateMemory vk-device mem-alloc nil long-p)
                (vulkan-utils/vk-check "failed to allocate memory"))
          vk-memory (.get long-p 0)]
      ;; todo v2 of vkbindimagememory
      (-> (VK12/vkBindImageMemory vk-device vk-image vk-memory 0)
          (vulkan-utils/vk-check "failed to bind image memory"))
      (assoc this :vk-image vk-image :vk-memory vk-memory))))

(defn- -stop
  [{:keys [device vk-image vk-memory], :as this}]
  (println "stopping attachment")
  (let [vk-device (vk.device/get-vk-device device)]
    (VK12/vkDestroyImage vk-device vk-image nil)
    (VK12/vkFreeMemory vk-device vk-memory nil))
  this)

(defn- -get-format [{{:keys [format]} :image-data}] format)

(defn- -get-mip-levels [{{:keys [mip-levels]} :image-data}] mip-levels)

(defn- -get-vk-image [{:keys [vk-image]}] vk-image)

(defn- -get-vk-memory [{:keys [vk-memory]}] vk-memory)

(defrecord Image [device image-data]
  ImageI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (get-format [this] (-get-format this))
    (get-mip-levels [this] (-get-mip-levels this))
    (get-vk-image [this] (-get-vk-image this))
    (get-vk-memory [this] (-get-vk-memory this)))


(defrecord ImageData [array-layers format height mip-levels sample-count usage
                      width])

(defn create-image-data
  [overrides]
  (map->ImageData (merge {:format VK12/VK_FORMAT_R8G8B8A8_SRGB,
                          :mip-levels 1,
                          :sample-count 1,
                          :array-layers 1}
                         overrides)))
