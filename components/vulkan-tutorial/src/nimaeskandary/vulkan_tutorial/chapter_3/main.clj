(ns nimaeskandary.vulkan-tutorial.chapter-3.main
  (:require [nimaeskandary.vulkan-tutorial.chapter-3.eng.engine :as engine]
            [nimaeskandary.vulkan-tutorial.chapter-3.eng.proto.app-logic :as
             proto.app-logic]
            [nimaeskandary.vulkan-tutorial.chapter-3.eng.proto.engine :as
             proto.engine]))

(defn start
  [this]
  (let [engine (proto.engine/start (engine/->Engine "Vulkan Book" this))]
    (proto.engine/init engine)))

(defrecord Main []
  proto.app-logic/AppLogic
    (start [this] (start this))
    (stop [_this])
    (input [_this _window _scene _diff-time-millis])
    (init [_this _window _scene _render])
    (update-fn [_this _window _scene _diff-time-millis]))

(defn -main [] (proto.app-logic/start (->Main)))
