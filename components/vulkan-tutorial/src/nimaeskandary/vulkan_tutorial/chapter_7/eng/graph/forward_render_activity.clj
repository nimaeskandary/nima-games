(ns nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.forward-render-activity
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.config :as config]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.image-view :as
     vk.image-view]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.queue :as vk.queue]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.semaphore :as
     vk.semaphore]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.shader-program :as
     vk.shader-program]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.swap-chain :as
     vk.swap-chain]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.swap-chain-render-pass
     :as vk.swap-chain-render-pass]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.command-buffer :as
     vk.command-buffer]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.fence :as vk.fence]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.frame-buffer :as
     vk.frame-buffer]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.sync-semaphores :as
     vk.sync-semaphores]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.pipeline :as
     vk.pipeline]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.vertex-input-state-info
     :as vk.vertex-input-state-info]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.vertex-buffer-structure
     :as vk.vertex-buffer-structure]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.vulkan-buffer :as
     vk.vulkan-buffer]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vulkan-model :as
     graph.vulkan-model])
  (:import (java.util.function Consumer)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.util.shaderc Shaderc)
           (org.lwjgl.vulkan VK12
                             VkClearValue
                             VkCommandBuffer
                             VkExtent2D
                             VkOffset2D
                             VkRect2D
                             VkRenderPassBeginInfo
                             VkViewport)))

(def fragment-shader-file:glsl "vulkan-tutorial/shaders/fwd_fragment.glsl")
(def fragment-shader-file:spv "app-output/shaders/fwd_fragment.spv")
(def vertex-shader-file:glsl "vulkan-tutorial/shaders/fwd_vertex.glsl")
(def vertex-shader-file:spv "app-output/shaders/fwd_vertex.spv")

(defprotocol ForwardRenderActivityI
  (start [this])
  (stop [this])
  (submit [this queue])
  (wait-for-fence [this])
  (record-command-buffer [this vulkan-model-list]))

(defn -start
  [{:keys [swap-chain command-pool pipeline-cache], :as this}]
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
          _ (when (:shader-recompilation? config/config)
              (vk.shader-program/compile-shader-if-changed
               vertex-shader-file:glsl
               vertex-shader-file:spv
               Shaderc/shaderc_glsl_vertex_shader)
              (vk.shader-program/compile-shader-if-changed
               fragment-shader-file:glsl
               fragment-shader-file:spv
               Shaderc/shaderc_glsl_fragment_shader))
          fwd-shader-program (-> (vk.shader-program/->ShaderProgram
                                  device
                                  [(vk.shader-program/->ShaderModuleData
                                    VK12/VK_SHADER_STAGE_VERTEX_BIT
                                    vertex-shader-file:spv)
                                   (vk.shader-program/->ShaderModuleData
                                    VK12/VK_SHADER_STAGE_FRAGMENT_BIT
                                    fragment-shader-file:spv)])
                                 vk.shader-program/start)
          vertex-buffer-structure
          (-> (vk.vertex-buffer-structure/->VertexBufferStructure)
              vk.vertex-input-state-info/start)
          pipeline-create-info (vk.pipeline/->PipelineCreateInfo
                                (vk.swap-chain-render-pass/get-vk-render-pass
                                 render-pass)
                                fwd-shader-program
                                1
                                vertex-buffer-structure)
          pipeline (-> (vk.pipeline/->Pipeline pipeline-cache
                                               pipeline-create-info)
                       vk.pipeline/start)
          _ (vk.vertex-input-state-info/stop vertex-buffer-structure)
          {:keys [command-buffers fences]}
          (doall (reduce (fn [result _]
                           (let [cb (-> (vk.command-buffer/->CommandBuffer
                                         command-pool
                                         true
                                         false)
                                        vk.command-buffer/start)
                                 fence (-> (vk.fence/->Fence device true)
                                           vk.fence/start)]
                             (-> result
                                 (update :command-buffers conj cb)
                                 (update :fences conj fence))))
                         {:command-buffers [], :fences []}
                         (range num-images)))]
      (assoc this
             :command-buffers command-buffers
             :fences fences
             :frame-buffers frame-buffers
             :render-pass render-pass
             :pipeline pipeline
             :fwd-shader-program fwd-shader-program))))

(defn -stop
  [{:keys [frame-buffers render-pass command-buffers fences pipeline
           fwd-shader-program],
    :as this}]
  (println "stopping forward rendering activity")
  (vk.pipeline/stop pipeline)
  (vk.shader-program/stop fwd-shader-program)
  (doseq [fb frame-buffers] (vk.frame-buffer/stop fb))
  (vk.swap-chain-render-pass/stop render-pass)
  (doseq [cb command-buffers] (vk.command-buffer/stop cb))
  (doseq [f fences] (vk.fence/stop f))
  this)

(defn -submit
  [{:keys [swap-chain fences command-buffers]} queue]
  (with-open [stack (MemoryStack/stackPush)]
    (let [idx (vk.swap-chain/get-current-frame swap-chain)
          command-buffer (nth command-buffers idx)
          current-fence (nth fences idx)
          _ (vk.fence/reset current-fence)
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

(defn- -record-command-buffer
  [{:keys [swap-chain command-buffers frame-buffers render-pass pipeline]}
   vulkan-model-list]
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkExtent2D swap-chain-extent (vk.swap-chain/get-swap-chain-extent
                                         swap-chain)
          width (.width swap-chain-extent)
          height (.height swap-chain-extent)
          idx (vk.swap-chain/get-current-frame swap-chain)
          command-buffer (nth command-buffers idx)
          frame-buffer (nth frame-buffers idx)
          _ (vk.command-buffer/reset command-buffer)
          clear-values (VkClearValue/calloc 1 stack)
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
          _ (vk.command-buffer/begin-recording command-buffer)
          vk-command-buffer (vk.command-buffer/get-vk-command-buffer
                             command-buffer)
          _ (VK12/vkCmdBeginRenderPass vk-command-buffer
                                       begin-info
                                       VK12/VK_SUBPASS_CONTENTS_INLINE)
          _ (VK12/vkCmdBindPipeline vk-command-buffer
                                    VK12/VK_PIPELINE_BIND_POINT_GRAPHICS
                                    (vk.pipeline/get-vk-pipeline pipeline))
          viewport (-> (VkViewport/calloc 1 stack)
                       (.x 0)
                       (.y height)
                       (.height (- height))
                       (.width width)
                       (.minDepth 0.0)
                       (.maxDepth 1.0))
          _ (VK12/vkCmdSetViewport vk-command-buffer 0 viewport)
          scissor (-> (VkRect2D/calloc 1 stack)
                      (.extent (reify
                                Consumer
                                  (accept [_ it]
                                    (-> ^VkExtent2D it
                                        (.width width)
                                        (.height height)))))
                      (.offset (reify
                                Consumer
                                  (accept [_ it]
                                    (-> ^VkOffset2D it
                                        (.x 0)
                                        (.y 0))))))
          _ (VK12/vkCmdSetScissor vk-command-buffer 0 scissor)
          offsets (.mallocLong stack 1)
          _ (.put offsets 0 0)
          vertex-b (.mallocLong stack 1)]
      (doseq [vulkan-model vulkan-model-list]
        (doseq [{:keys [vertices-buffer indices-buffer num-indices]}
                @(graph.vulkan-model/get-vulkan-mesh-list-atom vulkan-model)]
          (.put vertex-b 0 (vk.vulkan-buffer/get-buffer vertices-buffer))
          (VK12/vkCmdBindVertexBuffers vk-command-buffer 0 vertex-b offsets)
          (VK12/vkCmdBindIndexBuffer vk-command-buffer
                                     (vk.vulkan-buffer/get-buffer
                                      indices-buffer)
                                     0
                                     VK12/VK_INDEX_TYPE_UINT32)
          (VK12/vkCmdDrawIndexed vk-command-buffer num-indices 1 0 0 0)))
      (VK12/vkCmdEndRenderPass vk-command-buffer)
      (vk.command-buffer/end-recording command-buffer))))

(defrecord ForwardRenderActivity [swap-chain command-pool pipeline-cache]
  ForwardRenderActivityI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (submit [this queue] (-submit this queue))
    (wait-for-fence [this] (-wait-for-fence this))
    (record-command-buffer [this vulkan-model-list]
      (-record-command-buffer this vulkan-model-list)))
