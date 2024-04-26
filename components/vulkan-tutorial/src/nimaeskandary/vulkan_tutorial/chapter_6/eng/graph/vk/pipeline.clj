(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.pipeline
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.device :as vk.device]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.shader-program :as
     vk.shader-program]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.pipeline-cache :as
     vk.pipeline-cache]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.vertex-input-state-info
     :as vk.vertex-input-state-info]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.vulkan-utils :as
     vulkan-utils])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK12
                             VkDevice
                             VkGraphicsPipelineCreateInfo
                             VkPipelineColorBlendAttachmentState
                             VkPipelineColorBlendStateCreateInfo
                             VkPipelineDynamicStateCreateInfo
                             VkPipelineInputAssemblyStateCreateInfo
                             VkPipelineLayoutCreateInfo
                             VkPipelineMultisampleStateCreateInfo
                             VkPipelineRasterizationStateCreateInfo
                             VkPipelineShaderStageCreateInfo
                             VkPipelineViewportStateCreateInfo)))

(defprotocol PipelineI
  (start [this])
  (stop [this])
  (get-vk-pipeline ^Long [this])
  (get-vk-pipeline-layout ^Long [this]))

(defrecord PipelineCreateInfo [^Long vk-render-pass shader-program
                               ^Long num-color-attachments vi-input-state-info])

(defn- -start
  [{:keys [pipeline-cache],
    {:keys [vk-render-pass shader-program num-color-attachments
            vi-input-state-info]}
    :pipeline-create-info,
    :as this}]
  (println "creating pipeline")
  (with-open [stack (MemoryStack/stackPush)]
    (let [long-p (.mallocLong stack 1)
          main (.UTF8 stack "main")
          shader-modules (vk.shader-program/get-shader-modules shader-program)
          num-modules (count shader-modules)
          shader-stages (VkPipelineShaderStageCreateInfo/calloc num-modules
                                                                stack)
          _ (doseq [^Integer i (range num-modules)]
              (let [{:keys [shader-stage handle]} (nth shader-modules i)]
                (-> ^VkPipelineShaderStageCreateInfo (.get shader-stages i)
                    .sType$Default
                    (.stage shader-stage)
                    (.module handle)
                    (.pName main))))
          input-assembly-state-crate-info
          (-> (VkPipelineInputAssemblyStateCreateInfo/calloc stack)
              .sType$Default
              (.topology VK12/VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST))
          viewport-state-create-info
          (-> (VkPipelineViewportStateCreateInfo/calloc stack)
              .sType$Default
              (.viewportCount 1)
              (.scissorCount 1))
          rasterization-state-create-info
          (-> (VkPipelineRasterizationStateCreateInfo/calloc stack)
              .sType$Default
              (.polygonMode VK12/VK_POLYGON_MODE_FILL)
              (.cullMode VK12/VK_CULL_MODE_NONE)
              (.frontFace VK12/VK_FRONT_FACE_CLOCKWISE)
              (.lineWidth 1.0))
          multisample-state-create-info
          (-> (VkPipelineMultisampleStateCreateInfo/calloc stack)
              .sType$Default
              (.rasterizationSamples VK12/VK_SAMPLE_COUNT_1_BIT))
          blend-attachment-state (VkPipelineColorBlendAttachmentState/calloc
                                  num-color-attachments
                                  stack)
          _ (doseq [^Integer i (range num-color-attachments)]
              (-> ^VkPipelineColorBlendAttachmentState
                  (.get blend-attachment-state i)
                  (.colorWriteMask (bit-or VK12/VK_COLOR_COMPONENT_R_BIT
                                           VK12/VK_COLOR_COMPONENT_G_BIT
                                           VK12/VK_COLOR_COMPONENT_B_BIT
                                           VK12/VK_COLOR_COMPONENT_A_BIT))))
          color-blend-state (-> (VkPipelineColorBlendStateCreateInfo/calloc
                                 stack)
                                .sType$Default
                                (.pAttachments blend-attachment-state))
          dynamic-state-create-info
          (-> (VkPipelineDynamicStateCreateInfo/calloc stack)
              .sType$Default
              (.pDynamicStates (.ints stack
                                      VK12/VK_DYNAMIC_STATE_VIEWPORT
                                      VK12/VK_DYNAMIC_STATE_SCISSOR)))
          p-pipeline-layout-create-info (-> (VkPipelineLayoutCreateInfo/calloc
                                             stack)
                                            .sType$Default)
          ^VkDevice vk-device (-> (vk.pipeline-cache/get-device pipeline-cache)
                                  vk.device/get-vk-device)
          _ (-> (VK12/vkCreatePipelineLayout vk-device
                                             p-pipeline-layout-create-info
                                             nil
                                             long-p)
                (vulkan-utils/vk-check "failed to create pipeline layout"))
          vk-pipeline-layout (.get long-p 0)
          pipeline (-> (VkGraphicsPipelineCreateInfo/calloc 1 stack)
                       .sType$Default
                       (.pStages shader-stages)
                       (.pVertexInputState (vk.vertex-input-state-info/get-vi
                                            vi-input-state-info))
                       (.pInputAssemblyState input-assembly-state-crate-info)
                       (.pViewportState viewport-state-create-info)
                       (.pRasterizationState rasterization-state-create-info)
                       (.pMultisampleState multisample-state-create-info)
                       (.pColorBlendState color-blend-state)
                       (.pDynamicState dynamic-state-create-info)
                       (.layout vk-pipeline-layout)
                       (.renderPass vk-render-pass))
          _ (-> (VK12/vkCreateGraphicsPipelines
                 vk-device
                 (vk.pipeline-cache/get-vk-pipeline-cache pipeline-cache)
                 pipeline
                 nil
                 long-p)
                (vulkan-utils/vk-check "error creating graphics pipeline"))
          vk-pipeline (.get long-p 0)]
      (assoc this
             :vk-pipeline vk-pipeline
             :vk-pipeline-layout vk-pipeline-layout))))

(defn -stop
  [{:keys [pipeline-cache vk-pipeline vk-pipeline-layout]}]
  (println "stopping pipeline")
  (let [vk-device (-> (vk.pipeline-cache/get-device pipeline-cache)
                      vk.device/get-vk-device)]
    (VK12/vkDestroyPipelineLayout vk-device vk-pipeline-layout nil)
    (VK12/vkDestroyPipeline vk-device vk-pipeline nil)))

(defn -get-vk-pipeline [{:keys [vk-pipeline]}] vk-pipeline)

(defn -get-vk-pipeline-layout [{:keys [vk-pipeline-layout]}] vk-pipeline-layout)

(defrecord Pipeline [pipeline-cache pipeline-create-info]
  PipelineI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (get-vk-pipeline [this] (-get-vk-pipeline this))
    (get-vk-pipeline-layout [this] (-get-vk-pipeline-layout this)))
