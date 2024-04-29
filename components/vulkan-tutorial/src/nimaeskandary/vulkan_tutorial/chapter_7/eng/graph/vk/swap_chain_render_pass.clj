(ns nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.swap-chain-render-pass
  (:require [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.device :as
             vk.device]
            [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.swap-chain :as
             vk.swap-chain]
            [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.vulkan-utils
             :as vulkan-utils])
  (:import (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan KHRSwapchain
                             VK12
                             VkAttachmentDescription
                             VkAttachmentReference
                             VkDevice
                             VkRenderPassCreateInfo
                             VkSubpassDependency
                             VkSubpassDescription)))

(defprotocol SwapChainRenderPassI
  (start [this])
  (stop [this])
  (get-vk-render-pass [this]))

(defn -start
  [{:keys [swap-chain depth-image-format], :as this}]
  (println "starting render pass")
  (with-open [stack (MemoryStack/stackPush)]
    ;; todo version 2 of these objects
    (let [attachments (VkAttachmentDescription/calloc 2 stack)
          ;; color attachment
          _ (-> ^VkAttachmentDescription (.get attachments 0)
                (.format (-> (vk.swap-chain/get-surface-format swap-chain)
                             :image-format))
                (.samples VK12/VK_SAMPLE_COUNT_1_BIT)
                (.loadOp VK12/VK_ATTACHMENT_LOAD_OP_CLEAR)
                (.storeOp VK12/VK_ATTACHMENT_STORE_OP_STORE)
                (.initialLayout VK12/VK_IMAGE_LAYOUT_UNDEFINED)
                (.finalLayout KHRSwapchain/VK_IMAGE_LAYOUT_PRESENT_SRC_KHR))
          color-ref (-> (VkAttachmentReference/calloc 1 stack)
                        (.attachment 0)
                        (.layout VK12/VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL))
          ;; depth attachment
          _ (-> ^VkAttachmentDescription (.get attachments 1)
                (.format depth-image-format)
                (.samples VK12/VK_SAMPLE_COUNT_1_BIT)
                (.loadOp VK12/VK_ATTACHMENT_LOAD_OP_CLEAR)
                (.storeOp VK12/VK_ATTACHMENT_STORE_OP_DONT_CARE)
                (.initialLayout VK12/VK_IMAGE_LAYOUT_UNDEFINED)
                (.finalLayout
                 VK12/VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL))
          depth-ref (-> (VkAttachmentReference/malloc stack)
                        (.attachment 1)
                        (.layout
                         VK12/VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL))
          sub-pass (-> (VkSubpassDescription/calloc 1 stack)
                       (.pipelineBindPoint VK12/VK_PIPELINE_BIND_POINT_GRAPHICS)
                       (.colorAttachmentCount (.remaining color-ref))
                       (.pColorAttachments color-ref)
                       (.pDepthStencilAttachment depth-ref))
          sub-pass-deps (VkSubpassDependency/calloc 1 stack)
          _ (-> ^VkSubpassDependency (.get sub-pass-deps 0)
                (.srcSubpass VK12/VK_SUBPASS_EXTERNAL)
                (.dstSubpass 0)
                (.srcStageMask
                 (bit-or VK12/VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                         VK12/VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT))
                (.dstStageMask
                 (bit-or VK12/VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                         VK12/VK_PIPELINE_STAGE_EARLY_FRAGMENT_TESTS_BIT))
                (.srcAccessMask 0)
                (.dstAccessMask
                 (bit-or VK12/VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT
                         VK12/VK_ACCESS_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT)))
          render-pass-info (VkRenderPassCreateInfo/calloc stack)
          _ (-> render-pass-info
                .sType$Default
                (.pAttachments attachments)
                (.pSubpasses sub-pass)
                (.pDependencies sub-pass-deps))
          ^VkDevice vk-device (-> (vk.swap-chain/get-device swap-chain)
                                  vk.device/get-vk-device)
          long-b (.mallocLong stack 1)
          _ (-> (VK12/vkCreateRenderPass vk-device render-pass-info nil long-b)
                (vulkan-utils/vk-check "failed to create render pass"))
          vk-render-pass (.get long-b 0)]
      (assoc this :vk-render-pass vk-render-pass))))

(defn -stop
  [{:keys [swap-chain vk-render-pass], :as this}]
  (println "stopping render pass")
  (let [vk-device (-> (vk.swap-chain/get-device swap-chain)
                      vk.device/get-vk-device)]
    (VK12/vkDestroyRenderPass vk-device vk-render-pass nil))
  this)

(defn -get-vk-render-pass [{:keys [vk-render-pass]}] vk-render-pass)

(defrecord SwapChainRenderPass [swap-chain depth-image-format]
  SwapChainRenderPassI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (get-vk-render-pass [this] (-get-vk-render-pass this)))
