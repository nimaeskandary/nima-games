(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.frame-buffer
  (:require [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.vulkan-utils
             :as vulkan-utils]
            [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.device :as
             proto.device]
            [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.frame-buffer :as
             proto.frame-buffer])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK12 VkDevice VkFramebufferCreateInfo)))

(defn start
  [{:keys [device width height p-attachments render-pass], :as this}]
  (println "starting frame buffer")
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkDevice vk-device (proto.device/get-vk-device device)
          frame-buff-create-info (-> (VkFramebufferCreateInfo/calloc stack)
                                     (.sType$Default)
                                     (.pAttachments p-attachments)
                                     (.width width)
                                     (.height height)
                                     (.layers 1)
                                     (.renderPass render-pass))
          long-b (.mallocLong stack 1)
          _ (-> (VK12/vkCreateFramebuffer vk-device
                                          frame-buff-create-info
                                          nil
                                          long-b)
                (vulkan-utils/vk-check "failed to create frame buffer"))
          vk-frame-buffer (.get long-b 0)]
      (assoc this :vk-frame-buffer vk-frame-buffer))))

(defn stop
  [{:keys [device vk-frame-buffer]}]
  (println "stopping frame buffer")
  (let [vk-device (proto.device/get-vk-device device)]
    (VK12/vkDestroyFramebuffer vk-device vk-frame-buffer nil)))

(defn get-vk-frame-buffer [{:keys [vk-frame-buffer]}] vk-frame-buffer)

(defrecord FrameBuffer [device width height p-attachments render-pass]
  proto.frame-buffer/FrameBuffer
    (start [this] (start this))
    (stop [this] (stop this))
    (get-vk-frame-buffer [this] (get-vk-frame-buffer this)))
