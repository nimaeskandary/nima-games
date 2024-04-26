(ns nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vulkan-mesh
  (:require [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.vulkan-buffer
             :as vk.vulkan-buffer]))

(defprotocol VulkanMeshI
  (start [this])
  (stop [this]))

(defn- -start [this] this)

(defn- -stop
  [{:keys [vertices-buffer indices-buffer], :as this}]
  (println "stopping vulkan mesh")
  (vk.vulkan-buffer/stop vertices-buffer)
  (vk.vulkan-buffer/stop indices-buffer)
  this)

(defrecord VulkanMesh [vertices-buffer indices-buffer num-indices]
  VulkanMeshI
    (start [this] (-start this))
    (stop [this] (-stop this)))
