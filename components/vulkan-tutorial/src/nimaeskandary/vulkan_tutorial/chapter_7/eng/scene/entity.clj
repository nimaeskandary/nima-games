(ns nimaeskandary.vulkan-tutorial.chapter-7.eng.scene.entity
  (:import (org.joml Quaternionf Matrix4f Vector3f)))

(defprotocol EntityI
  (start [this])
  (stop [this])
  (get-id [this])
  (get-model-id [this])
  (get-model-matrix ^Matrix4f [this])
  (get-position [this])
  (get-rotation ^Quaternionf [this])
  (get-scale [this])
  (reset-rotation [this])
  (set-position [this x y z])
  (set-scale [this scale])
  (update-model-matrix [this]))

(defn- -start
  [{:keys [id model-id], :as this}]
  (println (format "starting entity id: %s model-id: %s" id model-id))
  (let [this (assoc this
                    :scale-atom (atom 1.0)
                    :rotation (Quaternionf.)
                    :model-matrix (Matrix4f.))]
    (update-model-matrix this)
    this))

(defn- -stop [this] (println "stopping entity") this)

(defn- -get-id [{:keys [id]}] id)

(defn- -get-model-id [{:keys [model-id]}] model-id)

(defn- -get-model-matrix [{:keys [model-matrix]}] model-matrix)

(defn- -get-position [{:keys [position]}] position)

(defn- -get-rotation [{:keys [rotation]}] rotation)

(defn- -get-scale [{:keys [scale-atom]}] @scale-atom)

(defn- -reset-rotation
  [{:keys [^Quaternionf rotation]}]
  (set! (.-x rotation) 0.0)
  (set! (.-y rotation) 0.0)
  (set! (.-z rotation) 0.0)
  (set! (.-w rotation) 1.0))

(defn- -set-position
  [{:keys [^Vector3f position]} x y z]
  (set! (.-x position) x)
  (set! (.-y position) y)
  (set! (.-z position) z))

(defn- -set-scale
  [{:keys [scale-atom], :as this} scale]
  (reset! scale-atom scale)
  (update-model-matrix this))

(defn- -update-model-matrix
  [{:keys [^Matrix4f model-matrix ^Vector3f position scale-atom
           ^Quaternionf rotation]}]
  (let [^Float scale @scale-atom]
    (.translationRotateScale model-matrix position rotation scale)))

(defrecord Entity [^String id ^String model-id ^Vector3f position]
  EntityI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (get-id [this] (-get-id this))
    (get-model-id [this] (-get-model-id this))
    (get-model-matrix [this] (-get-model-matrix this))
    (get-position [this] (-get-position this))
    (get-rotation [this] (-get-rotation this))
    (get-scale [this] (-get-scale this))
    (reset-rotation [this] (-reset-rotation this))
    (set-position [this x y z] (-set-position this x y z))
    (set-scale [this scale] (-set-scale this scale))
    (update-model-matrix [this] (-update-model-matrix this)))
