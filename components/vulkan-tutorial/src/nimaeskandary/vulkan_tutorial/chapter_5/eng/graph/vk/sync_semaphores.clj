(ns nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.sync-semaphores
  (:require [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.semaphore :as
             proto.semaphore]
            [nimaeskandary.vulkan-tutorial.chapter-5.eng.graph.vk.semaphore :as
             vk.semaphore]
            [nimaeskandary.vulkan-tutorial.chapter-5.eng.proto.sync-semaphores
             :as proto.sync-semaphores]))

(defn start
  [{:keys [device], :as this}]
  (println "starting sync semaphore")
  (assoc this
         :image-acquisition-semaphore (proto.semaphore/start
                                       (vk.semaphore/->Semaphore device))
         :render-complete-semaphore (proto.semaphore/start
                                     (vk.semaphore/->Semaphore device))))

(defn stop
  [{:keys [image-acquisition-semaphore render-complete-semaphore], :as this}]
  (println "stopping sync semaphore")
  (proto.semaphore/stop image-acquisition-semaphore)
  (proto.semaphore/stop render-complete-semaphore)
  this)

(defn get-image-acquisition-semaphore
  [{:keys [image-acquisition-semaphore]}]
  image-acquisition-semaphore)

(defn get-render-complete-semaphore
  [{:keys [render-complete-semaphore]}]
  render-complete-semaphore)

(defrecord SyncSemaphores [device]
  proto.sync-semaphores/SyncSemaphores
    (start [this] (start this))
    (stop [this] (stop this))
    (get-image-acquisition-semaphore [this]
      (get-image-acquisition-semaphore this))
    (get-render-complete-semaphore [this] (get-render-complete-semaphore this)))
