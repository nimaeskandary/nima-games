(ns nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.frame-buffer
  (:require [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.device :as
             vk.device]
            [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.vulkan-utils
             :as vulkan-utils])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK12 VkDevice VkFramebufferCreateInfo)))

(defprotocol FrameBufferI
  (start [this])
  (stop [this])
  (get-vk-frame-buffer [this]))

(defn -start
  [{:keys [device width height p-attachments vk-render-pass], :as this}]
  (println "starting frame buffer")
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkDevice vk-device (vk.device/get-vk-device device)
          frame-buff-create-info (-> (VkFramebufferCreateInfo/calloc stack)
                                     (.sType$Default)
                                     (.pAttachments p-attachments)
                                     (.width width)
                                     (.height height)
                                     (.layers 1)
                                     (.renderPass vk-render-pass))
          long-b (.mallocLong stack 1)
          _ (-> (VK12/vkCreateFramebuffer vk-device
                                          frame-buff-create-info
                                          nil
                                          long-b)
                (vulkan-utils/vk-check "failed to create frame buffer"))
          vk-frame-buffer (.get long-b 0)]
      (assoc this :vk-frame-buffer vk-frame-buffer))))

(defn -stop
  [{:keys [device vk-frame-buffer]}]
  (println "stopping frame buffer")
  (let [vk-device (vk.device/get-vk-device device)]
    (VK12/vkDestroyFramebuffer vk-device vk-frame-buffer nil)))

(defn -get-vk-frame-buffer [{:keys [vk-frame-buffer]}] vk-frame-buffer)

(defrecord FrameBuffer [device width height p-attachments vk-render-pass]
  FrameBufferI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (get-vk-frame-buffer [this] (-get-vk-frame-buffer this)))
