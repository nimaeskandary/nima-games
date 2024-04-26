(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vulkan-model
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.fence :as vk.fence]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.command-buffer :as
     vk.command-buffer]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.command-pool :as
     vk.command-pool]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.graph-constants :as
     vk.graph-constants]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.queue :as vk.queue]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.vulkan-buffer :as
     vk.vulkan-buffer]
    [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vulkan-mesh :as
     graph.vulkan-mesh])
  (:import (org.lwjgl.system MemoryStack MemoryUtil)
           (org.lwjgl.vulkan VK12 VkBufferCopy VkCommandBuffer)))

(defprotocol VulkanModelI
  (start [this])
  (stop [this])
  (get-model-id [this])
  (get-vulkan-mesh-list-atom [this]))

(defrecord TransferBuffers [src-buffer dst-buffer])

(defn -start
  [this]
  (println "starting vulkan model")
  (assoc this :vulkan-mesh-list-atom (atom [])))

(defn -stop
  [{:keys [vulkan-mesh-list-atom]}]
  (println "stopping vulkan model")
  (doseq [vk-mesh @vulkan-mesh-list-atom] (graph.vulkan-mesh/stop vk-mesh)))

(defn -get-model-id [{:keys [model-id]}] (model-id))

(defn -get-vulkan-mesh-list-atom
  [{:keys [vulkan-mesh-list-atom]}]
  vulkan-mesh-list-atom)

(defrecord VulkanModel [model-id]
  VulkanModelI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (get-model-id [this] (-get-model-id this))
    (get-vulkan-mesh-list-atom [this] (-get-vulkan-mesh-list-atom this)))

(defn- create-indices-buffers
  [device {:keys [^ints indices], :as _mesh-data}]
  (let [buffer-size (* (alength indices) vk.graph-constants/int-length)
        src-buffer (-> (vk.vulkan-buffer/map->VulkanBuffer
                        {:device device,
                         :requested-size buffer-size,
                         :usage VK12/VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                         :req-mask (bit-or
                                    VK12/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    VK12/VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)})
                       vk.vulkan-buffer/start)
        dst-buffer (-> (vk.vulkan-buffer/map->VulkanBuffer
                        {:device device,
                         :requested-size buffer-size,
                         :usage (bit-or VK12/VK_BUFFER_USAGE_TRANSFER_DST_BIT
                                        VK12/VK_BUFFER_USAGE_INDEX_BUFFER_BIT),
                         :req-mask VK12/VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT})
                       vk.vulkan-buffer/start)
        mapped-memory (vk.vulkan-buffer/map-memory src-buffer)
        data (MemoryUtil/memIntBuffer mapped-memory
                                      (vk.vulkan-buffer/get-requested-size
                                       src-buffer))]
    (.put data indices)
    (vk.vulkan-buffer/unmap-memory src-buffer)
    (->TransferBuffers src-buffer dst-buffer)))

(defn- create-vertices-buffers
  [device {:keys [^floats positions], :as _mesh-data}]
  (let [buffer-size (* (alength positions) vk.graph-constants/float-length)
        src-buffer (-> (vk.vulkan-buffer/map->VulkanBuffer
                        {:device device,
                         :requested-size buffer-size,
                         :usage VK12/VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                         :req-mask (bit-or
                                    VK12/VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT
                                    VK12/VK_MEMORY_PROPERTY_HOST_COHERENT_BIT)})
                       vk.vulkan-buffer/start)
        dst-buffer (-> (vk.vulkan-buffer/map->VulkanBuffer
                        {:device device,
                         :requested-size buffer-size,
                         :usage (bit-or VK12/VK_BUFFER_USAGE_TRANSFER_DST_BIT
                                        VK12/VK_BUFFER_USAGE_VERTEX_BUFFER_BIT),
                         :req-mask VK12/VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT})
                       vk.vulkan-buffer/start)
        mapped-memory (vk.vulkan-buffer/map-memory src-buffer)
        data (MemoryUtil/memFloatBuffer mapped-memory
                                        (vk.vulkan-buffer/get-requested-size
                                         src-buffer))]
    (.put data positions)
    (vk.vulkan-buffer/unmap-memory src-buffer)
    (->TransferBuffers src-buffer dst-buffer)))

(defn- record-transfer-command
  [cmd {:keys [src-buffer dst-buffer], :as _transfer-buffers}]
  (with-open [stack (MemoryStack/stackPush)]
    ;; todo there is a v2 of VkBufferCopy
    (let [copy-region (-> (VkBufferCopy/calloc 1 stack)
                          (.srcOffset 0)
                          (.dstOffset 0)
                          (.size (vk.vulkan-buffer/get-requested-size
                                  src-buffer)))]
      (VK12/vkCmdCopyBuffer (vk.command-buffer/get-vk-command-buffer cmd)
                            (vk.vulkan-buffer/get-buffer src-buffer)
                            (vk.vulkan-buffer/get-buffer dst-buffer)
                            copy-region))))

(defn transform-models
  [model-data-list command-pool queue]
  (let [device (vk.command-pool/get-device command-pool)
        cmd (-> (vk.command-buffer/->CommandBuffer command-pool true true)
                vk.command-buffer/start)
        vk-model-list-atom (atom [])
        staging-buffer-list-atom (atom [])]
    (vk.command-buffer/begin-recording cmd)
    (doseq [{:keys [model-id mesh-data-list]} model-data-list]
      (let [vk-model (start (->VulkanModel model-id))]
        (swap! vk-model-list-atom conj vk-model)
        ;; transform meshes loading their data into GPU buffers
        (doseq [{:keys [indices], :as mesh-data} mesh-data-list]
          (let [vertices-buffers (create-vertices-buffers device mesh-data)
                indices-buffers (create-indices-buffers device mesh-data)]
            (swap! staging-buffer-list-atom conj
              (:src-buffer vertices-buffers)
              (:src-buffer indices-buffers))
            (record-transfer-command cmd vertices-buffers)
            (record-transfer-command cmd indices-buffers)
            (->> (graph.vulkan-mesh/->VulkanMesh (:dst-buffer vertices-buffers)
                                                 (:dst-buffer indices-buffers)
                                                 (alength indices))
                 graph.vulkan-mesh/start
                 (swap! (get-vulkan-mesh-list-atom vk-model) conj))))))
    (vk.command-buffer/end-recording cmd)
    (let [fence (-> (vk.fence/->Fence device true)
                    vk.fence/start)]
      (vk.fence/reset fence)
      (with-open [stack (MemoryStack/stackPush)]
        (vk.queue/submit queue
                         (.pointers stack
                                    ^VkCommandBuffer
                                    (vk.command-buffer/get-vk-command-buffer
                                     cmd))
                         nil
                         nil
                         nil
                         fence))
      (vk.fence/fence-wait fence)
      (vk.fence/stop fence))
    (vk.command-buffer/stop cmd)
    (doseq [vk-buffer @staging-buffer-list-atom]
      (vk.vulkan-buffer/stop vk-buffer))
    @vk-model-list-atom))
