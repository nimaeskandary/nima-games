(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.image-view
  (:require [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.vulkan-utils
             :as vulkan-utils]
            [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.device :as
             proto.device]
            [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.image-view :as
             proto.image-view])
  (:import (java.util.function Consumer)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK12
                             VkDevice
                             VkImageSubresourceRange
                             VkImageViewCreateInfo)))

(defn start
  [{:keys [device vk-image],
    {:keys [view-type format mip-levels base-array-layer layer-count
            aspect-mask]}
    :image-view-data,
    :as this}]
  (println "starting image view")
  (with-open [stack (MemoryStack/stackPush)]
    (let [long-b (.mallocLong stack 1)
          view-create-info (-> (VkImageViewCreateInfo/calloc stack)
                               (.sType$Default)
                               (.image vk-image)
                               (.viewType view-type)
                               (.format format)
                               (.subresourceRange
                                (reify
                                 Consumer
                                   (accept [_ it]
                                     (doto ^VkImageSubresourceRange it
                                       (.aspectMask aspect-mask)
                                       (.baseMipLevel 0)
                                       (.levelCount mip-levels)
                                       (.baseArrayLayer base-array-layer)
                                       (.layerCount layer-count))))))
          ^VkDevice vk-device (proto.device/get-vk-device device)]
      (-> (VK12/vkCreateImageView vk-device view-create-info nil long-b)
          (vulkan-utils/vk-check "failed to create image view"))
      (assoc this :vk-image-view (.get long-b 0)))))

(defn stop
  [{:keys [device vk-image-view], :as this}]
  (VK12/vkDestroyImageView (proto.device/get-vk-device device)
                           vk-image-view
                           nil)
  this)

(defn get-aspect-mask
  [{:keys [image-view-data]}]
  (:aspect-mask image-view-data))

(defn get-mip-levels [{:keys [image-view-data]}] (:mip-levels image-view-data))

(defn get-vk-image-view [{:keys [vk-image-view]}] vk-image-view)

(defrecord ImageView [device vk-image image-view-data]
  proto.image-view/ImageView
    (start [this] (start this))
    (stop [this] (stop this))
    (get-aspect-mask [this] (get-aspect-mask this))
    (get-mip-levels [this] (get-mip-levels this))
    (get-vk-image-view [this] (get-vk-image-view this)))

(defrecord ImageViewData [format aspect-mask base-array-layer layer-count
                          mip-levels view-type])

(defn create-image-view-data
  [format aspect-mask]
  (map->ImageViewData {:format format,
                       :aspect-mask aspect-mask,
                       :base-array-layer 0,
                       :layer-count 1,
                       :mip-levels 1,
                       :view-type VK12/VK_IMAGE_VIEW_TYPE_2D}))
