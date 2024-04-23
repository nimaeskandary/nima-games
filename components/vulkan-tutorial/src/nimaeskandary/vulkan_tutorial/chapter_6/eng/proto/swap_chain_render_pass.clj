(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.swap-chain-render-pass)

(defprotocol SwapChainRenderPass
  (start [this])
  (stop [this])
  (get-vk-render-pass [this]))
