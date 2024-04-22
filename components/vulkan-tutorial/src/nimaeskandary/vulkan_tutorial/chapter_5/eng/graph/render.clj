(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.render
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.command-pool :as
     proto.command-pool]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.forward-render-activity
     :as proto.forward-render-activity]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.queue :as proto.queue]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.swap-chain :as
     proto.swap-chain]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.window :as proto.window]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.device :as proto.device]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.physical-device :as
     proto.physical-device]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.render :as proto.render]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.instance :as
     proto.instance]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.instance :as
     vk.instance]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.physical-device :as
     vk.physical-device]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.device :as vk.device]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.surface :as
     vk.surface]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.swap-chain :as
     vk.swap-chain]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.command-pool :as
     vk.command-pool]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.queue :as vk.queue]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.forward-render-activity
     :as forward-render-activity]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.config :as config]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.surface :as
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
        present-queue (vk.queue/create-presentation-queue device surface 0)
        graphics-queue (vk.queue/create-graphics-queue device 0)
        swap-chain (proto.swap-chain/start (vk.swap-chain/->SwapChain
                                            device
                                            surface
                                            window
                                            requested-images
                                            vsync?
                                            present-queue
                                            [graphics-queue]))
        command-pool (proto.command-pool/start
                      (vk.command-pool/->CommandPool
                       device
                       (proto.queue/get-queue-family-index graphics-queue)))
        fwd-render-activity (proto.forward-render-activity/start
                             (forward-render-activity/->ForwardRenderActivity
                              swap-chain
                              command-pool))]
    (assoc this
           :instance instance
           :physical-device physical-device
           :device device
           :surface surface
           :graphics-queue graphics-queue
           :present-queue present-queue
           :swap-chain swap-chain
           :command-pool command-pool
           :fwd-render-activity fwd-render-activity)))

(defn stop
  [{:keys [present-queue graphics-queue fwd-render-activity command-pool
           swap-chain surface device physical-device instance],
    :as this}]
  (println "stopping render")
  (proto.queue/wait-idle present-queue)
  (proto.queue/wait-idle graphics-queue)
  (proto.device/wait-idle device)
  (proto.forward-render-activity/stop fwd-render-activity)
  (proto.command-pool/stop command-pool)
  (proto.swap-chain/stop swap-chain)
  (proto.surface/stop surface)
  (proto.device/stop device)
  (proto.physical-device/stop physical-device)
  (proto.instance/stop instance)
  this)

(defn render
  [{:keys [fwd-render-activity swap-chain graphics-queue present-queue]}]
  (proto.forward-render-activity/wait-for-fence fwd-render-activity)
  (proto.swap-chain/acquire-next-image swap-chain)
  (proto.forward-render-activity/submit fwd-render-activity graphics-queue)
  (proto.swap-chain/present-image swap-chain present-queue))

(defrecord Render [window scene]
  proto.render/Render
    (start [this] (start this))
    (stop [this] (stop this))
    (render [this] (render this)))
