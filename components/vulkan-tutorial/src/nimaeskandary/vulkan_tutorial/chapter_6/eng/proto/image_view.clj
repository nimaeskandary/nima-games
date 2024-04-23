(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.image-view)

(defprotocol ImageView
  (start [this])
  (stop [this])
  (get-aspect-mask [this])
  (get-mip-levels [this])
  (get-vk-image-view ^Long [this]))
