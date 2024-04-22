(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.forward-render-activity
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.command-buffer :as
     proto.command-buffer]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.fence :as proto.fence]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.frame-buffer :as
     proto.frame-buffer]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.image-view :as
     proto.image-view]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.queue :as proto.queue]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.semaphore :as
     proto.semaphore]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.swap-chain :as
     proto.swap-chain]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.swap-chain-render-pass
     :as vk.swap-chain-render-pass]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.command-buffer :as
     vk.command-buffer]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.fence :as vk.fence]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.frame-buffer :as
     vk.frame-buffer]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.swap-chain-render-pass
     :as proto.swap-chain-render-pass]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.sync-semaphores :as
     proto.sync-semaphores]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.forward-render-activity
     :as proto.forward-render-activity])
  (:import (java.util.function Consumer)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK12
                             VkClearValue
                             VkCommandBuffer
                             VkExtent2D
                             VkRect2D
                             VkRenderPassBeginInfo)))

(defn record-command-buffer
  [render-pass command-buffer frame-buffer width height]
  (with-open [stack (MemoryStack/stackPush)]
    (let [clear-values (VkClearValue/calloc 1 stack)
          _ (.apply clear-values
                    0
                    (reify
                     Consumer
                       (accept [_ it]
                         (-> ^VkClearValue it
                             .color
                             (.float32 0 0.5)
                             (.float32 1 0.7)
                             (.float32 2 0.9)
                             (.float32 3 1)))))
          begin-info (-> (VkRenderPassBeginInfo/calloc stack)
                         .sType$Default
                         (.renderPass
                          (proto.swap-chain-render-pass/get-vk-render-pass
                           render-pass))
                         (.pClearValues clear-values)
                         (.renderArea (reify
                                       Consumer
                                         (accept [_ it]
                                           (-> ^VkRect2D it
                                               .extent
                                               (.set width height)))))
                         (.framebuffer (proto.frame-buffer/get-vk-frame-buffer
                                        frame-buffer)))
          vk-command-buffer (proto.command-buffer/get-vk-command-buffer
                             command-buffer)]
      (proto.command-buffer/begin-recording command-buffer)
      (VK12/vkCmdBeginRenderPass vk-command-buffer
                                 begin-info
                                 VK12/VK_SUBPASS_CONTENTS_INLINE)
      (VK12/vkCmdEndRenderPass vk-command-buffer)
      (proto.command-buffer/end-recording command-buffer))))

(defn start
  [{:keys [swap-chain command-pool], :as this}]
  (println "starting forward rendering activity")
  (with-open [stack (MemoryStack/stackPush)]
    (let [device (proto.swap-chain/get-device swap-chain)
          ^VkExtent2D swap-chain-extent (proto.swap-chain/get-swap-chain-extent
                                         swap-chain)
          image-views (proto.swap-chain/get-image-views swap-chain)
          num-images (count image-views)
          render-pass (proto.swap-chain-render-pass/start
                       (vk.swap-chain-render-pass/->SwapChainRenderPass
                        swap-chain))
          attachments-b (.mallocLong stack 1)
          frame-buffers
          (doall (for [image-view image-views]
                   (let [vk-image-view (proto.image-view/get-vk-image-view
                                        image-view)]
                     (.put attachments-b 0 vk-image-view)
                     (-> (vk.frame-buffer/map->FrameBuffer
                          {:device device,
                           :width (.width swap-chain-extent),
                           :height (.height swap-chain-extent),
                           :p-attachments attachments-b,
                           :render-pass
                           (proto.swap-chain-render-pass/get-vk-render-pass
                            render-pass)})
                         proto.frame-buffer/start))))
          {:keys [command-buffers fences]}
          (doall
           (reduce (fn [result i]
                     (let [cb (-> (vk.command-buffer/->CommandBuffer
                                   command-pool
                                   true
                                   false)
                                  proto.command-buffer/start)
                           fence (-> (vk.fence/->Fence device true)
                                     proto.fence/start)
                           result (-> result
                                      (update :command-buffers conj cb)
                                      (update :fences conj fence))]
                       (record-command-buffer render-pass
                                              cb
                                              (nth frame-buffers i)
                                              (.width swap-chain-extent)
                                              (.height swap-chain-extent))
                       result))
                   {:command-buffers [], :fences []}
                   (range num-images)))]
      (assoc this
             :command-buffers command-buffers
             :fences fences
             :frame-buffers frame-buffers
             :render-pass render-pass))))

(defn stop
  [{:keys [frame-buffers render-pass command-buffers fences], :as this}]
  (println "stopping forward rendering activity")
  (doseq [fb frame-buffers] (proto.frame-buffer/stop fb))
  (proto.swap-chain-render-pass/stop render-pass)
  (doseq [cb command-buffers] (proto.command-buffer/stop cb))
  (doseq [f fences] (proto.fence/stop f))
  this)

(defn submit
  [{:keys [swap-chain fences command-buffers]} queue]
  (with-open [stack (MemoryStack/stackPush)]
    (let [idx (proto.swap-chain/get-current-frame swap-chain)
          current-fence (nth fences idx)
          _ (proto.fence/reset current-fence)
          command-buffer (nth command-buffers idx)
          sync-semaphores (-> (proto.swap-chain/get-sync-semaphores swap-chain)
                              (nth idx))
          vk-image-acquisition-semaphore
          (-> sync-semaphores
              proto.sync-semaphores/get-image-acquisition-semaphore
              proto.semaphore/get-vk-semaphore)
          vk-render-complete-semaphore
          (-> sync-semaphores
              proto.sync-semaphores/get-render-complete-semaphore
              proto.semaphore/get-vk-semaphore)]
      (proto.queue/submit
       queue
       (.pointers stack
                  ^VkCommandBuffer
                  (proto.command-buffer/get-vk-command-buffer command-buffer))
       (.longs stack vk-image-acquisition-semaphore)
       (.ints stack VK12/VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
       (.longs stack vk-render-complete-semaphore)
       current-fence))))

(defn wait-for-fence
  [{:keys [swap-chain fences]}]
  (let [idx (proto.swap-chain/get-current-frame swap-chain)
        current-fence (nth fences idx)]
    (proto.fence/fence-wait current-fence)))

(defrecord ForwardRenderActivity [swap-chain command-pool]
  proto.forward-render-activity/ForwardRenderActivity
    (start [this] (start this))
    (stop [this] (stop this))
    (submit [this queue] (submit this queue))
    (wait-for-fence [this] (wait-for-fence this)))
