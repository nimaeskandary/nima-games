(ns nimaeskandary.vulkan-tutorial.chapter-3.eng.graph.vk.surface
  (:require [nimaeskandary.vulkan-tutorial.chapter-3.eng.proto.physical-device
             :as proto.physical-device]
            [nimaeskandary.vulkan-tutorial.chapter-3.eng.proto.surface :as
             proto.surface])
  (:import (org.lwjgl.glfw GLFWVulkan)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan KHRSurface VkPhysicalDevice)))

(defn start
  [{:keys [physical-device ^Long window-handle], :as this}]
  (println "creating vulkan surface")
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkPhysicalDevice vk-physical-device
          (proto.physical-device/get-vk-physical-device physical-device)
          surface-p (.mallocLong stack 1)
          _ (GLFWVulkan/glfwCreateWindowSurface (.getInstance
                                                 vk-physical-device)
                                                window-handle
                                                nil
                                                surface-p)
          vk-surface (.get surface-p 0)]
      (assoc this :physical-device physical-device :vk-surface vk-surface))))

(defn stop
  [{:keys [physical-device vk-surface], :as this}]
  (println "destroying vulkan surface")
  (-> (proto.physical-device/get-vk-physical-device physical-device)
      .getInstance
      (KHRSurface/vkDestroySurfaceKHR vk-surface nil))
  this)

(defn get-vk-surface [{:keys [vk-surface]}] vk-surface)

(defrecord Surface [physical-device window-handle]
  proto.surface/Surface
    (start [this] (start this))
    (stop [this] (stop this))
    (get-vk-surface [this] (get-vk-surface this)))
