(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.proto.app-logic)

(defprotocol AppLogic
  (start [this])
  (stop [this])
  (input [this window scene diff-time-millis])
  (init [this window scene render])
  (update-fn [this window scene diff-time-millis]))
