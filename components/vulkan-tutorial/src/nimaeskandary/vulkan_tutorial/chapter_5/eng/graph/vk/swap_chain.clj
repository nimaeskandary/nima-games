(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.swap-chain
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.vulkan-utils :as
     vulkan-utils]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.image-view :as
     vk.image-view]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.sync-semaphores :as
     vk.sync-semaphores]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.device :as proto.device]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.physical-device :as
     proto.physical-device]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.queue :as proto.queue]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.semaphore :as
     proto.semaphore]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.surface :as
     proto.surface]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.sync-semaphores :as
     proto.sync-semaphores]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.window :as proto.window]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.image-view :as
     proto.image-view]
    [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.swap-chain :as
     proto.swap-chain])
  (:import (org.lwjgl.system MemoryStack MemoryUtil)
           (org.lwjgl.vulkan KHRSurface
                             KHRSwapchain
                             VK12
                             VkDevice
                             VkExtent2D
                             VkPhysicalDevice
                             VkPresentInfoKHR
                             VkQueue
                             VkSurfaceCapabilitiesKHR
                             VkSurfaceFormatKHR
                             VkSwapchainCreateInfoKHR)))

(defrecord SurfaceFormat [image-format color-space])

(defn calc-num-images
  [^VkSurfaceCapabilitiesKHR surf-capabilities ^Integer requested-images]
  (let [;; 0 means unlimited
        max-images (.maxImageCount surf-capabilities)
        min-images (.minImageCount surf-capabilities)
        result (if (pos? max-images)
                 (Math/max (Math/min requested-images max-images) min-images)
                 min-images)]
    (println (format "requested %d images, got %d images. max: %d, min: %d"
                     requested-images
                     result
                     max-images
                     min-images))
    result))

(defn calc-surface-format
  [^VkPhysicalDevice vk-physical-device ^Long vk-surface]
  (with-open [stack (MemoryStack/stackPush)]
    (let [int-p (.mallocInt stack 1)
          _ (-> (KHRSurface/vkGetPhysicalDeviceSurfaceFormatsKHR
                 vk-physical-device
                 vk-surface
                 int-p
                 nil)
                (vulkan-utils/vk-check
                 "failed to get number of surface formats"))
          num-formats (.get int-p 0)
          _ (when (not (pos? num-formats))
              (throw (Exception. "no surface formats retrieved")))
          surface-formats (VkSurfaceFormatKHR/calloc num-formats stack)
          _ (-> (KHRSurface/vkGetPhysicalDeviceSurfaceFormatsKHR
                 vk-physical-device
                 vk-surface
                 int-p
                 surface-formats)
                (vulkan-utils/vk-check "failed to get surface formats"))
          default-image-format VK12/VK_FORMAT_B8G8R8A8_SRGB
          default-color-space (.colorSpace surface-formats)
          image-format (atom nil)
          color-space (atom nil)]
      (doseq [^Integer i (range num-formats)
              :while (and (nil? @image-format) (nil? @color-space))]
        (let [sf ^VkSurfaceFormatKHR (.get surface-formats i)
              f (.format sf)
              cs (.colorSpace sf)]
          (when (and (= VK12/VK_FORMAT_B8G8R8A8_SRGB f)
                     (= KHRSurface/VK_COLOR_SPACE_SRGB_NONLINEAR_KHR cs))
            (reset! image-format f)
            (reset! color-space cs))))
      (->SurfaceFormat (or @image-format default-image-format)
                       (or @color-space default-color-space)))))

(defn calc-swap-chain-extent
  ^VkExtent2D [window ^VkSurfaceCapabilitiesKHR surf-capabilities]
  (let [result (VkExtent2D/calloc)]
    (if (= 0xFFFFFFFF
           (-> surf-capabilities
               .currentExtent
               .width))
      ;; surface size undefined, set to window size if within bounds
      (let [width (Math/min ^Integer (proto.window/get-width window)
                            (-> surf-capabilities
                                .maxImageExtent
                                .width))
            width (Math/max width
                            (-> surf-capabilities
                                .minImageExtent
                                .width))
            height (Math/min ^Integer (proto.window/get-height window)
                             (-> surf-capabilities
                                 .maxImageExtent
                                 .height))
            height (Math/max height
                             (-> surf-capabilities
                                 .minImageExtent
                                 .height))]
        (.width result width)
        (.height result height))
      ;; surface already defined, just use that for the swap chain
      (.set result (.currentExtent surf-capabilities)))
    result))

(defn create-image-views
  [^MemoryStack stack device ^Long swap-chain ^Integer format]
  (let [int-b (.mallocInt stack 1)
        ^VkDevice vk-device (proto.device/get-vk-device device)
        _
        (->
          (KHRSwapchain/vkGetSwapchainImagesKHR vk-device swap-chain int-b nil)
          (vulkan-utils/vk-check "failed to get number of surface images"))
        num-images (.get int-b 0)
        swap-chain-images (.mallocLong stack num-images)
        _ (-> (KHRSwapchain/vkGetSwapchainImagesKHR vk-device
                                                    swap-chain
                                                    int-b
                                                    swap-chain-images)
              (vulkan-utils/vk-check "failed to get surface images"))
        image-view-data (vk.image-view/create-image-view-data
                         format
                         VK12/VK_IMAGE_ASPECT_COLOR_BIT)]
    (doall (map (fn [^Integer i]
                  (proto.image-view/start (vk.image-view/->ImageView
                                           device
                                           (.get swap-chain-images i)
                                           image-view-data)))
                (range num-images)))))

(defn start
  [{:keys [device surface window requested-images vsync? presentation-queue
           concurrent-queues],
    :as this}]
  (println "starting vulkan swap chain")
  (with-open [stack (MemoryStack/stackPush)]
    (let [physical-device (proto.device/get-physical-device device)
          vk-physical-device (proto.physical-device/get-vk-physical-device
                              physical-device)
          vk-surface (proto.surface/get-vk-surface surface)
          ;; get surface capabilities
          surf-capabilities (VkSurfaceCapabilitiesKHR/calloc stack)
          _ (-> (KHRSurface/vkGetPhysicalDeviceSurfaceCapabilitiesKHR
                 vk-physical-device
                 vk-surface
                 surf-capabilities)
                (vulkan-utils/vk-check "failed to get surface capabilities"))
          num-images (calc-num-images surf-capabilities requested-images)
          surface-format (calc-surface-format vk-physical-device vk-surface)
          swap-chain-extent (calc-swap-chain-extent window surf-capabilities)
          present-mode (if vsync?
                         KHRSurface/VK_PRESENT_MODE_FIFO_KHR
                         KHRSurface/VK_PRESENT_MODE_IMMEDIATE_KHR)
          vk-swap-chain-create-info
          (-> (VkSwapchainCreateInfoKHR/calloc stack)
              (.sType$Default)
              (.surface vk-surface)
              (.minImageCount num-images)
              (.imageFormat (:image-format surface-format))
              (.imageColorSpace (:color-space surface-format))
              (.imageExtent swap-chain-extent)
              (.imageArrayLayers 1)
              (.imageUsage VK12/VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT)
              (.preTransform (.currentTransform surf-capabilities))
              (.compositeAlpha KHRSurface/VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
              (.clipped true)
              (.presentMode present-mode))
          presentation-queue-index (proto.queue/get-queue-family-index
                                    presentation-queue)
          indices (->> concurrent-queues
                       (reduce (fn [results cq]
                                 (let [cq-index
                                       (proto.queue/get-queue-family-index cq)]
                                   (if (not= presentation-queue-index cq-index)
                                     (conj results cq-index)
                                     results)))
                               [])
                       doall)
          _ (if (pos? (count indices))
              (let [int-b (.mallocInt stack (inc (count indices)))]
                (doseq [^Integer i indices] (.put int-b i))
                (-> (.put int-b presentation-queue-index)
                    .flip)
                (-> vk-swap-chain-create-info
                    (.imageSharingMode VK12/VK_SHARING_MODE_CONCURRENT)
                    (.queueFamilyIndexCount (.capacity int-b))
                    (.pQueueFamilyIndices int-b)))
              (.imageSharingMode vk-swap-chain-create-info
                                 VK12/VK_SHARING_MODE_EXCLUSIVE))
          long-b (.mallocLong stack 1)
          _ (-> (KHRSwapchain/vkCreateSwapchainKHR (proto.device/get-vk-device
                                                    device)
                                                   vk-swap-chain-create-info
                                                   nil
                                                   long-b)
                (vulkan-utils/vk-check "failed to create swap chain"))
          vk-swap-chain (.get long-b 0)
          image-views (create-image-views stack
                                          device
                                          vk-swap-chain
                                          (:image-format surface-format))
          sync-semaphores (doall (for [_ image-views]
                                   (proto.sync-semaphores/start
                                    (vk.sync-semaphores/->SyncSemaphores
                                     device))))]
      (assoc this
             :image-views image-views
             :surface-format surface-format
             :swap-chain-extent swap-chain-extent
             :vk-swap-chain vk-swap-chain
             :sync-semaphores sync-semaphores
             :current-frame-atom (atom 0)))))

(defn stop
  [{:keys [swap-chain-extent image-views device vk-swap-chain sync-semaphores],
    :as this}]
  (println "destroying vulkan swap chain")
  (.free swap-chain-extent)
  (doseq [image-view image-views] (proto.image-view/stop image-view))
  (doseq [sync-semaphore sync-semaphores]
    (proto.sync-semaphores/stop sync-semaphore))
  (KHRSwapchain/vkDestroySwapchainKHR (proto.device/get-vk-device device)
                                      vk-swap-chain
                                      nil)
  this)

(defn acquire-next-image
  [{:keys [device sync-semaphores current-frame-atom ^Long vk-swap-chain]}]
  (with-open [stack (MemoryStack/stackPush)]
    (let [vk-semaphore (-> (nth sync-semaphores @current-frame-atom)
                           proto.sync-semaphores/get-image-acquisition-semaphore
                           proto.semaphore/get-vk-semaphore)
          vk-device (proto.device/get-vk-device device)
          int-b (.mallocInt stack 1)
          status (KHRSwapchain/vkAcquireNextImageKHR vk-device
                                                     vk-swap-chain
                                                     0
                                                     vk-semaphore
                                                     MemoryUtil/NULL
                                                     int-b)
          resize? (atom false)]
      (cond (= KHRSwapchain/VK_ERROR_OUT_OF_DATE_KHR status) (reset! resize?
                                                               true)
            ;; not optimal but swapchain can still be used
            (= KHRSwapchain/VK_SUBOPTIMAL_KHR status) nil
            (not= VK12/VK_SUCCESS status)
            (throw (Exception. ^String
                               (format "failed to acquire image: %d" status))))
      (reset! current-frame-atom (.get int-b 0))
      @resize?)))

(defn present-image
  [{:keys [sync-semaphores ^Long vk-swap-chain current-frame-atom image-views]}
   queue]
  (with-open [stack (MemoryStack/stackPush)]
    (let [^VkQueue vk-queue (proto.queue/get-vk-queue queue)
          vk-semaphore (-> (nth sync-semaphores @current-frame-atom)
                           proto.sync-semaphores/get-render-complete-semaphore
                           proto.semaphore/get-vk-semaphore)
          present-info (-> (VkPresentInfoKHR/calloc stack)
                           .sType$Default
                           (.pWaitSemaphores (.longs stack vk-semaphore))
                           (.swapchainCount 1)
                           (.pSwapchains (.longs stack vk-swap-chain))
                           (.pImageIndices (.ints stack
                                                  (int @current-frame-atom))))
          status (KHRSwapchain/vkQueuePresentKHR vk-queue present-info)
          resize? (atom false)]
      (cond (= KHRSwapchain/VK_ERROR_OUT_OF_DATE_KHR status) (reset! resize?
                                                               true)
            (= KHRSwapchain/VK_SUBOPTIMAL_KHR status) nil
            (not= VK12/VK_SUCCESS status)
            (throw (Exception. ^String
                               (format "failed to present image: %d" status))))
      (reset! current-frame-atom (mod (inc @current-frame-atom)
                                      (count image-views))))))

(defn get-image-views [{:keys [image-views]}] image-views)

(defn get-num-images [{:keys [image-views]}] (count image-views))

(defn get-surface-format [{:keys [surface-format]}] surface-format)

(defn get-swap-chain-extent [{:keys [swap-chain-extent]}] swap-chain-extent)

(defn get-vk-swap-chain [{:keys [vk-swap-chain]}] vk-swap-chain)

(defn get-device [{:keys [device]}] device)

(defn get-current-frame [{:keys [current-frame-atom]}] @current-frame-atom)

(defn get-sync-semaphores [{:keys [sync-semaphores]}] sync-semaphores)

(defrecord SwapChain [device surface window requested-images vsync?
                      presentation-queue concurrent-queues]
  proto.swap-chain/SwapChain
    (start [this] (start this))
    (stop [this] (stop this))
    (get-image-views [this] (get-image-views this))
    (get-num-images [this] (get-num-images this))
    (get-surface-format [this] (get-surface-format this))
    (get-swap-chain-extent [this] (get-swap-chain-extent this))
    (get-vk-swap-chain [this] (get-vk-swap-chain this))
    (get-device [this] (get-device this))
    (acquire-next-image [this] (acquire-next-image this))
    (present-image [this queue] (present-image this queue))
    (get-current-frame [this] (get-current-frame this))
    (get-sync-semaphores [this] (get-sync-semaphores this)))
