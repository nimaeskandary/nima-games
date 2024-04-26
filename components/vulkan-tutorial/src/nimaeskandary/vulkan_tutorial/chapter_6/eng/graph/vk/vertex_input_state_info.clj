(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.vertex-input-state-info
  (:import (org.lwjgl.vulkan VkPipelineVertexInputStateCreateInfo)))

(defprotocol VertexInputStateInfoI
  (start [this])
  (stop [this])
  (get-vi ^VkPipelineVertexInputStateCreateInfo [this]))
