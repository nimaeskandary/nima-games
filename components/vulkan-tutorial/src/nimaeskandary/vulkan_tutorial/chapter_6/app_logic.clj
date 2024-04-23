(ns nimaeskandary.vulkan-tutorial.chapter-6.app-logic)

(defprotocol AppLogicI
  (start [this])
  (stop [this])
  (input [this window scene diff-time-millis])
  (init [this window scene render])
  (update-fn [this window scene diff-time-millis]))

(defrecord AppLogic []
  AppLogicI
    (start [this] this)
    (stop [this] this)
    (input [_this _window _scene _diff-time-millis])
    (init [_this _window _scene _render])
    (update-fn [_this _window _scene _diff-time-millis]))
