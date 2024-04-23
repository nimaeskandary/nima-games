(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.forward-render-activity
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.image-view :as
     vk.image-view]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.queue :as vk.queue]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.semaphore :as
     vk.semaphore]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.swap-chain :as
     vk.swap-chain]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.swap-chain-render-pass
     :as vk.swap-chain-render-pass]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.command-buffer :as
     vk.command-buffer]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.fence :as vk.fence]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.frame-buffer :as
     vk.frame-buffer]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.sync-semaphores :as
     vk.sync-semaphores])
  (:import (java.util.function Consumer)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.vulkan VK12
                             VkClearValue
                             VkCommandBuffer
                             VkExtent2D
                             VkRect2D
                             VkRenderPassBeginInfo)))

(defprotocol ForwardRenderActivityI
  (start [this])
  (stop [this])
  (submit [this queue])
  (wait-for-fence [this]))

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
          begin-info
          (-> (VkRenderPassBeginInfo/calloc stack)
              .sType$Default
              (.renderPass (vk.swap-chain-render-pass/get-vk-render-pass
                            render-pass))
              (.pClearValues clear-values)
              (.renderArea (reify
                            Consumer
                              (accept [_ it]
                                (-> ^VkRect2D it
                                    .extent
                                    (.set width height)))))
              (.framebuffer (vk.frame-buffer/get-vk-frame-buffer frame-buffer)))
          vk-command-buffer (vk.command-buffer/get-vk-command-buffer
                             command-buffer)]
      (vk.command-buffer/begin-recording command-buffer)
      (VK12/vkCmdBeginRenderPass vk-command-buffer
                                 begin-info
                                 VK12/VK_SUBPASS_CONTENTS_INLINE)
      (VK12/vkCmdEndRenderPass vk-command-buffer)
      (vk.command-buffer/end-recording command-buffer))))

(defn -start
  [{:keys [swap-chain command-pool], :as this}]
  (println "starting forward rendering activity")
  (with-open [stack (MemoryStack/stackPush)]
    (let [device (vk.swap-chain/get-device swap-chain)
          ^VkExtent2D swap-chain-extent (vk.swap-chain/get-swap-chain-extent
                                         swap-chain)
          image-views (vk.swap-chain/get-image-views swap-chain)
          num-images (count image-views)
          render-pass (vk.swap-chain-render-pass/start
                       (vk.swap-chain-render-pass/->SwapChainRenderPass
                        swap-chain))
          attachments-b (.mallocLong stack 1)
          frame-buffers
          (doall
           (for [image-view image-views]
             (let [vk-image-view (vk.image-view/get-vk-image-view image-view)]
               (.put attachments-b 0 vk-image-view)
               (-> (vk.frame-buffer/map->FrameBuffer
                    {:device device,
                     :width (.width swap-chain-extent),
                     :height (.height swap-chain-extent),
                     :p-attachments attachments-b,
                     :render-pass (vk.swap-chain-render-pass/get-vk-render-pass
                                   render-pass)})
                   vk.frame-buffer/start))))
          {:keys [command-buffers fences]}
          (doall
           (reduce (fn [result i]
                     (let [cb (-> (vk.command-buffer/->CommandBuffer
                                   command-pool
                                   true
                                   false)
                                  vk.command-buffer/start)
                           fence (-> (vk.fence/->Fence device true)
                                     vk.fence/start)
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

(defn -stop
  [{:keys [frame-buffers render-pass command-buffers fences], :as this}]
  (println "stopping forward rendering activity")
  (doseq [fb frame-buffers] (vk.frame-buffer/stop fb))
  (vk.swap-chain-render-pass/stop render-pass)
  (doseq [cb command-buffers] (vk.command-buffer/stop cb))
  (doseq [f fences] (vk.fence/stop f))
  this)

(defn -submit
  [{:keys [swap-chain fences command-buffers]} queue]
  (with-open [stack (MemoryStack/stackPush)]
    (let [idx (vk.swap-chain/get-current-frame swap-chain)
          current-fence (nth fences idx)
          _ (vk.fence/reset current-fence)
          command-buffer (nth command-buffers idx)
          sync-semaphores (-> (vk.swap-chain/get-sync-semaphores swap-chain)
                              (nth idx))
          vk-image-acquisition-semaphore
          (-> sync-semaphores
              vk.sync-semaphores/get-image-acquisition-semaphore
              vk.semaphore/get-vk-semaphore)
          vk-render-complete-semaphore
          (-> sync-semaphores
              vk.sync-semaphores/get-render-complete-semaphore
              vk.semaphore/get-vk-semaphore)]
      (vk.queue/submit
       queue
       (.pointers stack
                  ^VkCommandBuffer
                  (vk.command-buffer/get-vk-command-buffer command-buffer))
       (.longs stack vk-image-acquisition-semaphore)
       (.ints stack VK12/VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
       (.longs stack vk-render-complete-semaphore)
       current-fence))))

(defn -wait-for-fence
  [{:keys [swap-chain fences]}]
  (let [idx (vk.swap-chain/get-current-frame swap-chain)
        current-fence (nth fences idx)]
    (vk.fence/fence-wait current-fence)))

(defrecord ForwardRenderActivity [swap-chain command-pool]
  ForwardRenderActivityI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (submit [this queue] (-submit this queue))
    (wait-for-fence [this] (-wait-for-fence this)))
