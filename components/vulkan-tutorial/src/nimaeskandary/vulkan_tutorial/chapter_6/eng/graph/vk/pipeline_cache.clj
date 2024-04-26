(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.pipeline-cache
  (:require [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.device :as
             vk.device]
            [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.vulkan-utils
             :as vulkan-utils])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK12 VkDevice VkPipelineCacheCreateInfo)))

(defprotocol PipelineCacheI
  (start [this])
  (stop [this])
  (get-device [this])
  (get-vk-pipeline-cache ^Long [this]))

(defn- -start
  [{:keys [device], :as this}]
  (println "starting pipeline cache")
  (with-open [stack (MemoryStack/stackPush)]
    (let [create-info (-> (VkPipelineCacheCreateInfo/calloc stack)
                          .sType$Default)
          long-p (.mallocLong stack 1)
          _ (-> (VK12/vkCreatePipelineCache ^VkDevice
                                            (vk.device/get-vk-device device)
                                            create-info
                                            nil
                                            long-p)
                (vulkan-utils/vk-check "error creating pipeline cache"))
          vk-pipeline-cache (.get long-p 0)]
      (assoc this :vk-pipeline-cache vk-pipeline-cache))))

(defn- -stop
  [{:keys [device vk-pipeline-cache], :as this}]
  (println "stopping pipeline cache")
  (VK12/vkDestroyPipelineCache (vk.device/get-vk-device device)
                               vk-pipeline-cache
                               nil)
  this)

(defn- -get-device [{:keys [device]}] device)

(defn- -get-vk-pipeline-cache [{:keys [vk-pipeline-cache]}] vk-pipeline-cache)

(defrecord PipelineCache [device]
  PipelineCacheI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (get-device [this] (-get-device this))
    (get-vk-pipeline-cache [this] (-get-vk-pipeline-cache this)))
