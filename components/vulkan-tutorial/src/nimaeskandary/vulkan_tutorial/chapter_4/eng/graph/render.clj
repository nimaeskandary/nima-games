(ns nimaeskandary.vulkan-tutorial.chapter-4.eng.graph.render
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.proto.swap-chain :as
     proto.swap-chain]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.proto.window :as proto.window]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.proto.device :as proto.device]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.proto.physical-device :as
     proto.physical-device]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.proto.render :as proto.render]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.proto.instance :as
     proto.instance]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.graph.vk.instance :as
     vk.instance]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.graph.vk.physical-device :as
     vk.physical-device]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.graph.vk.device :as vk.device]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.graph.vk.surface :as
     vk.surface]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.graph.vk.swap-chain :as
     vk.swap-chain]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.graph.vk.queue :as vk.queue]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.config :as config]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.proto.surface :as
     proto.surface]))

(defn start
  [{:keys [window _scene], :as this}]
  (println "starting render")
  (let [{:keys [validate? physical-device-name vsync? requested-images]}
        config/config
        instance (proto.instance/start (vk.instance/->Instance validate?))
        physical-device (vk.physical-device/create-physical-device
                         instance
                         physical-device-name)
        device (proto.device/start (vk.device/->Device physical-device))
        surface (proto.surface/start (vk.surface/->Surface
                                      physical-device
                                      (proto.window/get-window-handle window)))
        graphics-queue (vk.queue/create-graphics-queue device 0)
        swap-chain (proto.swap-chain/start (vk.swap-chain/->SwapChain
                                            device
                                            surface
                                            window
                                            requested-images
                                            vsync?))]
    (assoc this
           :instance instance
           :physical-device physical-device
           :device device
           :surface surface
           :graphics-queue graphics-queue
           :swap-chain swap-chain)))

(defn stop
  [{:keys [swap-chain surface device physical-device instance], :as this}]
  (println "stopping render")
  (proto.swap-chain/stop swap-chain)
  (proto.surface/stop surface)
  (proto.device/stop device)
  (proto.physical-device/stop physical-device)
  (proto.instance/stop instance)
  this)

(defn render [_ _ _])

(defrecord Render [window scene]
  proto.render/Render
    (start [this] (start this))
    (stop [this] (stop this))
    (render [this window_ scene_] (render this window_ scene_)))
