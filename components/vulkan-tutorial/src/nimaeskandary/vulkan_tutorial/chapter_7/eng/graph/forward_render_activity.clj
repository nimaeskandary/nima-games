(ns nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.forward-render-activity
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.graph-constants :as
     vk.graph-constants]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.config :as config]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.image :as vk.image]
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
     graph.vulkan-model]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.vk.attachment :as
     vk.attachment]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.scene.entity :as scene.entity]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.scene.projection :as
     scene.projection]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.scene.scene :as scene.scene])
  (:import (java.nio ByteBuffer)
           (java.util.function Consumer)
           (org.joml Matrix4f)
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
  (record-command-buffer [this vulkan-model-list])
  (resize [this swap-chain]))

(defn- create-depth-attachments
  [swap-chain device]
  (let [num-images (vk.swap-chain/get-num-images swap-chain)
        ^VkExtent2D swap-chain-extent (vk.swap-chain/get-swap-chain-extent
                                       swap-chain)]
    (-> (for [_ (range num-images)]
          (-> (vk.attachment/map->Attachment
               {:device device,
                :width (.width swap-chain-extent),
                :height (.height swap-chain-extent),
                :format VK12/VK_FORMAT_D32_SFLOAT,
                :usage VK12/VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT})
              vk.attachment/start))
        doall)))

(defn- create-frame-buffers
  [swap-chain device depth-attachments render-pass]
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkExtent2D swap-chain-extent (vk.swap-chain/get-swap-chain-extent
                                         swap-chain)
          image-views (vk.swap-chain/get-image-views swap-chain)
          num-images (count image-views)
          p-attachments (.mallocLong stack 2)]
      (-> (for [^Integer i (range num-images)]
            (do (.put p-attachments
                      0
                      (-> (nth image-views i)
                          vk.image-view/get-vk-image-view))
                (.put p-attachments
                      1
                      (-> (nth depth-attachments i)
                          vk.attachment/get-image-view
                          vk.image-view/get-vk-image-view))
                (-> (vk.frame-buffer/map->FrameBuffer
                     {:device device,
                      :width (.width swap-chain-extent),
                      :height (.height swap-chain-extent),
                      :p-attachments p-attachments,
                      :vk-render-pass
                      (vk.swap-chain-render-pass/get-vk-render-pass
                       render-pass)})
                    vk.frame-buffer/start)))
          doall))))

(defn set-push-constants
  [pipeline ^VkCommandBuffer vk-command-buffer ^Matrix4f proj-matrix
   ^Matrix4f model-matrix ^ByteBuffer push-constant-buffer]
  (.get proj-matrix push-constant-buffer)
  (.get model-matrix vk.graph-constants/mat-4x4-size push-constant-buffer)
  (VK12/vkCmdPushConstants vk-command-buffer
                           (vk.pipeline/get-vk-pipeline-layout pipeline)
                           VK12/VK_SHADER_STAGE_VERTEX_BIT
                           0
                           push-constant-buffer))

(defn -start
  [{:keys [swap-chain-atom command-pool pipeline-cache], :as this}]
  (println "starting forward rendering activity")
  (let [device (vk.swap-chain/get-device @swap-chain-atom)
        image-views (vk.swap-chain/get-image-views @swap-chain-atom)
        num-images (count image-views)
        depth-attachments (create-depth-attachments @swap-chain-atom device)
        render-pass (vk.swap-chain-render-pass/start
                     (vk.swap-chain-render-pass/->SwapChainRenderPass
                      @swap-chain-atom
                      (-> (nth depth-attachments 0)
                          vk.attachment/get-image
                          vk.image/get-format)))
        frame-buffers (create-frame-buffers @swap-chain-atom
                                            device
                                            depth-attachments
                                            render-pass)
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
        pipeline-create-info
        (vk.pipeline/map->PipelineCreateInfo
         {:vk-render-pass (vk.swap-chain-render-pass/get-vk-render-pass
                           render-pass),
          :shader-program fwd-shader-program,
          :num-color-attachments 1,
          :vi-input-state-info vertex-buffer-structure,
          :has-depth-attachment? true,
          :push-constant-size (* 2 vk.graph-constants/mat-4x4-size)})
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
           :render-pass render-pass
           :pipeline pipeline
           :fwd-shader-program fwd-shader-program
           :depth-attachments-atom (atom depth-attachments)
           :frame-buffers-atom (atom frame-buffers))))

(defn -stop
  [{:keys [frame-buffers-atom depth-attachments-atom render-pass command-buffers
           fences pipeline fwd-shader-program],
    :as this}]
  (println "stopping forward rendering activity")
  (vk.pipeline/stop pipeline)
  (doseq [da @depth-attachments-atom] (vk.attachment/stop da))
  (vk.shader-program/stop fwd-shader-program)
  (doseq [fb @frame-buffers-atom] (vk.frame-buffer/stop fb))
  (vk.swap-chain-render-pass/stop render-pass)
  (doseq [cb command-buffers] (vk.command-buffer/stop cb))
  (doseq [f fences] (vk.fence/stop f))
  this)

(defn -submit
  [{:keys [swap-chain-atom fences command-buffers]} queue]
  (with-open [stack (MemoryStack/stackPush)]
    (let [idx (vk.swap-chain/get-current-frame @swap-chain-atom)
          command-buffer (nth command-buffers idx)
          current-fence (nth fences idx)
          _ (vk.fence/reset current-fence)
          sync-semaphores (-> (vk.swap-chain/get-sync-semaphores
                               @swap-chain-atom)
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
  [{:keys [swap-chain-atom fences]}]
  (let [idx (vk.swap-chain/get-current-frame @swap-chain-atom)
        current-fence (nth fences idx)]
    (vk.fence/fence-wait current-fence)))

(defn- -record-command-buffer
  [{:keys [swap-chain-atom command-buffers frame-buffers-atom render-pass
           pipeline scene]} vulkan-model-list]
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkExtent2D swap-chain-extent (vk.swap-chain/get-swap-chain-extent
                                         @swap-chain-atom)
          width (.width swap-chain-extent)
          height (.height swap-chain-extent)
          idx (vk.swap-chain/get-current-frame @swap-chain-atom)
          command-buffer (nth command-buffers idx)
          frame-buffer (nth @frame-buffers-atom idx)
          _ (vk.command-buffer/reset command-buffer)
          clear-values (VkClearValue/calloc 2 stack)
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
          _ (.apply clear-values
                    1
                    (reify
                     Consumer
                       (accept [_ it]
                         (-> ^VkClearValue it
                             .depthStencil
                             (.depth 1.0)))))
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
          vertex-b (.mallocLong stack 1)
          push-constants-b (.malloc stack
                                    (* 2 vk.graph-constants/mat-4x4-size))]
      (doseq [vulkan-model vulkan-model-list]
        (let [model-id (graph.vulkan-model/get-model-id vulkan-model)
              entities (scene.scene/get-entities-by-model-id scene model-id)]
          (when (not-empty entities)
            (doseq [{:keys [vertices-buffer indices-buffer num-indices]}
                    @(graph.vulkan-model/get-vulkan-mesh-list-atom
                      vulkan-model)]
              (.put vertex-b 0 (vk.vulkan-buffer/get-buffer vertices-buffer))
              (VK12/vkCmdBindVertexBuffers vk-command-buffer 0 vertex-b offsets)
              (VK12/vkCmdBindIndexBuffer vk-command-buffer
                                         (vk.vulkan-buffer/get-buffer
                                          indices-buffer)
                                         0
                                         VK12/VK_INDEX_TYPE_UINT32)
              (doseq [entity entities]
                (set-push-constants pipeline
                                    vk-command-buffer
                                    (-> (scene.scene/get-projection scene)
                                        scene.projection/get-projection-matrix)
                                    (scene.entity/get-model-matrix entity)
                                    push-constants-b)
                (VK12/vkCmdDrawIndexed vk-command-buffer
                                       num-indices
                                       1 0
                                       0 0))))))
      (VK12/vkCmdEndRenderPass vk-command-buffer)
      (vk.command-buffer/end-recording command-buffer))))

(defn- -resize
  [{:keys [swap-chain-atom frame-buffers-atom render-pass
           depth-attachments-atom]} swap-chain]
  (let [device (vk.swap-chain/get-device swap-chain)]
    (reset! swap-chain-atom swap-chain)
    (doseq [fb @frame-buffers-atom] (vk.frame-buffer/stop fb))
    (doseq [da @depth-attachments-atom] (vk.attachment/stop da))
    (reset! depth-attachments-atom (create-depth-attachments @swap-chain-atom
                                                             device))
    (reset! frame-buffers-atom (create-frame-buffers @swap-chain-atom
                                                     device
                                                     @depth-attachments-atom
                                                     render-pass))))

(defrecord ForwardRenderActivity [swap-chain-atom command-pool pipeline-cache
                                  scene]
  ForwardRenderActivityI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (submit [this queue] (-submit this queue))
    (wait-for-fence [this] (-wait-for-fence this))
    (record-command-buffer [this vulkan-model-list]
      (-record-command-buffer this vulkan-model-list))
    (resize [this swap-chain] (-resize this swap-chain)))
