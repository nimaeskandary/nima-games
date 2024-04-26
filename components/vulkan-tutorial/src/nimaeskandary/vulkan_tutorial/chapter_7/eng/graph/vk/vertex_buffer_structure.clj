(ns nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.vertex-buffer-structure
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.graph-constants :as
     vk.graph-constants]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.vertex-input-state-info
     :as vk.vertex-input-state-info])
  (:import (org.lwjgl.vulkan VK12
                             VkPipelineVertexInputStateCreateInfo
                             VkVertexInputAttributeDescription
                             VkVertexInputAttributeDescription$Buffer
                             VkVertexInputBindingDescription
                             VkVertexInputBindingDescription$Buffer)))

(def ^Integer number-of-attributes 1)
(def ^Integer position-components 3)

(defn -start
  [this]
  (println "starting vertex buffer structure")
  (let [vi-attrs (VkVertexInputAttributeDescription/calloc number-of-attributes)
        vi-bindings (VkVertexInputBindingDescription/calloc 1)
        vi (VkPipelineVertexInputStateCreateInfo/calloc)
        i 0]
    (-> ^VkVertexInputAttributeDescription (.get vi-attrs i)
        (.binding 0)
        (.location i)
        (.format VK12/VK_FORMAT_R32G32B32_SFLOAT)
        (.offset 0))
    (-> ^VkVertexInputBindingDescription (.get vi-bindings 0)
        (.binding 0)
        (.stride (* position-components vk.graph-constants/float-length))
        (.inputRate VK12/VK_VERTEX_INPUT_RATE_VERTEX))
    (assoc this
           :vi-attrs vi-attrs
           :vi-bindings vi-bindings
           :vi
           (-> vi
               (.sType
                VK12/VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
               (.pVertexAttributeDescriptions vi-attrs)
               (.pVertexBindingDescriptions vi-bindings)))))

(defn -stop
  [{:keys [^VkPipelineVertexInputStateCreateInfo vi
           ^VkVertexInputBindingDescription$Buffer vi-bindings
           ^VkVertexInputAttributeDescription$Buffer vi-attrs],
    :as this}]
  (println "stopping vulkan buffer structure")
  (.free vi)
  (.free vi-bindings)
  (.free vi-attrs)
  this)

(defn -get-vi [{:keys [vi]}] vi)

(defrecord VertexBufferStructure []
  vk.vertex-input-state-info/VertexInputStateInfoI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (get-vi [this] (-get-vi this)))
