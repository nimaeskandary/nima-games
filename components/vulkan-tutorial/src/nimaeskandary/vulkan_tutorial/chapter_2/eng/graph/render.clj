(ns nimaeskandary.vulkan-tutorial.chapter-2.eng.graph.render
  (:require [nimaeskandary.vulkan-tutorial.chapter-2.eng.proto.render :as
             proto.render]
            [nimaeskandary.vulkan-tutorial.chapter-2.eng.proto.instance :as
             proto.instance]
            [nimaeskandary.vulkan-tutorial.chapter-2.eng.graph.vk.instance :as
             vk.instance]
            [nimaeskandary.vulkan-tutorial.chapter-2.eng.config :as config]))

(defn start
  [this]
  (let [instance (proto.instance/start (vk.instance/->Instance
                                        (:validate? config/config)))]
    (assoc this :instance instance)))

(defn stop [this] (proto.instance/stop (:instance this)) this)

(defn render [_ _ _])

(defrecord Render [window scene]
  proto.render/Render
    (start [this] (start this))
    (stop [this] (stop this))
    (render [this window_ scene_] (render this window_ scene_)))
