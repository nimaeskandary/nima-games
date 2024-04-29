(ns nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.attachment
  (:require [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.image :as
             vk.image]
            [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.image-view :as
             vk.image-view])
  (:import (org.lwjgl.vulkan VK12)))

(defprotocol AttachmentI
  (start [this])
  (stop [this])
  (get-image [this])
  (get-image-view [this])
  (depth-attachment? [this]))

(defn- -start
  [{:keys [device width height format usage], :as this}]
  (println "starting attachment")
  (let [image-data (vk.image/create-image-data
                    {:width width,
                     :height height,
                     :usage (bit-or usage VK12/VK_IMAGE_USAGE_SAMPLED_BIT),
                     :format format})
        image (-> (vk.image/->Image device image-data)
                  vk.image/start)
        {:keys [aspect-mask depth-attachment?]}
        (cond
          (> (bit-and usage VK12/VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT) 0)
          {:aspect-mask VK12/VK_IMAGE_ASPECT_COLOR_BIT,
           :depth-attachment? false}
          (> (bit-and usage VK12/VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT) 0)
          {:aspect-mask VK12/VK_IMAGE_ASPECT_DEPTH_BIT, :depth-attachment? true}
          :else {:aspect-mask 0, :depth-attachment? false})]
    (assoc this
           :depth-attachment? depth-attachment?
           :image image
           :image-view
           (->> (vk.image-view/create-image-view-data (vk.image/get-format
                                                       image)
                                                      aspect-mask)
                (vk.image-view/->ImageView device (vk.image/get-vk-image image))
                vk.image-view/start))))

(defn -stop
  [{:keys [image image-view], :as this}]
  (println "stopping attachment")
  (vk.image-view/stop image-view)
  (vk.image/stop image)
  this)

(defn- -get-image [{:keys [image]}] image)

(defn- -get-image-view [{:keys [image-view]}] image-view)

(defn- -depth-attachment? [{:keys [depth-attachment?]}] depth-attachment?)

(defrecord Attachment [device width height format usage]
  AttachmentI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (get-image [this] (-get-image this))
    (get-image-view [this] (-get-image-view this))
    (depth-attachment? [this] (-depth-attachment? this)))
