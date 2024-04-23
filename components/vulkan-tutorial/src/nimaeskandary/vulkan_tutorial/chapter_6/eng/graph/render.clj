(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.render
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.instance :as
     vk.instance]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.physical-device :as
     vk.physical-device]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.device :as vk.device]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.surface :as
     vk.surface]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.swap-chain :as
     vk.swap-chain]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.command-pool :as
     vk.command-pool]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.queue :as vk.queue]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.forward-render-activity
     :as graph.forward-render-activity]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.config :as config]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.window :as eng.window]))

(defprotocol RenderI
  (start [this])
  (stop [this])
  (render [this]))

(defn -start
  [{:keys [window _scene], :as this}]
  (println "starting render")
  (let [{:keys [validate? physical-device-name vsync? requested-images]}
        config/config
        instance (vk.instance/start (vk.instance/->Instance validate?))
        physical-device (vk.physical-device/create-physical-device
                         instance
                         physical-device-name)
        device (vk.device/start (vk.device/->Device physical-device))
        surface (vk.surface/start (vk.surface/->Surface
                                   physical-device
                                   (eng.window/get-window-handle window)))
        present-queue (vk.queue/create-presentation-queue device surface 0)
        graphics-queue (vk.queue/create-graphics-queue device 0)
        swap-chain (vk.swap-chain/start (vk.swap-chain/->SwapChain
                                         device
                                         surface
                                         window
                                         requested-images
                                         vsync?
                                         present-queue
                                         [graphics-queue]))
        command-pool (vk.command-pool/start (vk.command-pool/->CommandPool
                                             device
                                             (vk.queue/get-queue-family-index
                                              graphics-queue)))
        fwd-render-activity
        (graph.forward-render-activity/start
         (graph.forward-render-activity/->ForwardRenderActivity swap-chain
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

(defn -stop
  [{:keys [present-queue graphics-queue fwd-render-activity command-pool
           swap-chain surface device physical-device instance],
    :as this}]
  (println "stopping render")
  (vk.queue/wait-idle present-queue)
  (vk.queue/wait-idle graphics-queue)
  (vk.device/wait-idle device)
  (graph.forward-render-activity/stop fwd-render-activity)
  (vk.command-pool/stop command-pool)
  (vk.swap-chain/stop swap-chain)
  (vk.surface/stop surface)
  (vk.device/stop device)
  (vk.physical-device/stop physical-device)
  (vk.instance/stop instance)
  this)

(defn -render
  [{:keys [fwd-render-activity swap-chain graphics-queue present-queue]}]
  (graph.forward-render-activity/wait-for-fence fwd-render-activity)
  (vk.swap-chain/acquire-next-image swap-chain)
  (graph.forward-render-activity/submit fwd-render-activity graphics-queue)
  (vk.swap-chain/present-image swap-chain present-queue))

(defrecord Render [window scene]
  RenderI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (render [this] (-render this)))
