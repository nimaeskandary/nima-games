(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.swap-chain-render-pass
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.vulkan-utils :as
     vulkan-utils]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.device :as proto.device]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.swap-chain :as
     proto.swap-chain]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.swap-chain-render-pass
     :as proto.swap-chain-render-pass])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan KHRSwapchain
                             VK12
                             VkAttachmentDescription
                             VkAttachmentReference
                             VkDevice
                             VkRenderPassCreateInfo
                             VkSubpassDependency
                             VkSubpassDescription)))

(defn start
  [{:keys [swap-chain], :as this}]
  (println "starting render pass")
  (with-open [stack (MemoryStack/stackPush)]
    ;; todo version 2 of these objects
    (let [attachments (VkAttachmentDescription/calloc 1 stack)
          _ (doto ^VkAttachmentDescription (.get attachments 0)
              (.format (-> (proto.swap-chain/get-surface-format swap-chain)
                           :image-format))
              (.samples VK12/VK_SAMPLE_COUNT_1_BIT)
              (.loadOp VK12/VK_ATTACHMENT_LOAD_OP_CLEAR)
              (.storeOp VK12/VK_ATTACHMENT_STORE_OP_STORE)
              (.initialLayout VK12/VK_IMAGE_LAYOUT_UNDEFINED)
              (.finalLayout KHRSwapchain/VK_IMAGE_LAYOUT_PRESENT_SRC_KHR))
          color-ref (VkAttachmentReference/calloc 1 stack)
          _ (doto color-ref
              (.attachment 0)
              (.layout VK12/VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL))
          sub-pass (VkSubpassDescription/calloc 1 stack)
          _ (doto sub-pass
              (.pipelineBindPoint VK12/VK_PIPELINE_BIND_POINT_GRAPHICS)
              (.colorAttachmentCount (-> (.remaining color-ref)))
              (.pColorAttachments color-ref))
          sub-pass-deps (VkSubpassDependency/calloc 1 stack)
          _ (doto ^VkSubpassDependency (.get sub-pass-deps 0)
              (.srcSubpass VK12/VK_SUBPASS_EXTERNAL)
              (.dstSubpass 0)
              (.srcStageMask VK12/VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
              (.dstStageMask VK12/VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
              (.srcAccessMask 0)
              (.dstAccessMask VK12/VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT))
          render-pass-info (VkRenderPassCreateInfo/calloc stack)
          _ (doto render-pass-info
              .sType$Default
              (.pAttachments attachments)
              (.pSubpasses sub-pass)
              (.pDependencies sub-pass-deps))
          ^VkDevice vk-device (-> (proto.swap-chain/get-device swap-chain)
                                  proto.device/get-vk-device)
          long-b (.mallocLong stack 1)
          _ (-> (VK12/vkCreateRenderPass vk-device render-pass-info nil long-b)
                (vulkan-utils/vk-check "failed to create render pass"))
          vk-render-pass (.get long-b 0)]
      (assoc this :vk-render-pass vk-render-pass))))

(defn stop
  [{:keys [swap-chain vk-render-pass], :as this}]
  (println "stopping render pass")
  (let [vk-device (-> (proto.swap-chain/get-device swap-chain)
                      proto.device/get-vk-device)]
    (VK12/vkDestroyRenderPass vk-device vk-render-pass nil))
  this)

(defn get-vk-render-pass [{:keys [vk-render-pass]}] vk-render-pass)

(defrecord SwapChainRenderPass [swap-chain]
  proto.swap-chain-render-pass/SwapChainRenderPass
    (start [this] (start this))
    (stop [this] (stop this))
    (get-vk-render-pass [this] (get-vk-render-pass this)))
