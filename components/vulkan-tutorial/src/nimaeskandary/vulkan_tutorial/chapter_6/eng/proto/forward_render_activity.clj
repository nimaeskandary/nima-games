(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.forward-render-activity)

(defprotocol ForwardRenderActivity
  (start [this])
  (stop [this])
  (submit [this queue])
  (wait-for-fence [this]))
