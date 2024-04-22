(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.frame-buffer)

(defprotocol FrameBuffer
  (start [this])
  (stop [this])
  (get-vk-frame-buffer [this]))
