(ns nimaeskandary.vulkan-tutorial.chapter-8.eng.scene.projection
  (:require [nimaeskandary.vulkan-tutorial.chapter-8.eng.config :as config])
  (:import (org.joml Matrix4f)))

(defprotocol ProjectionI
  (start [this])
  (stop [this])
  (get-projection-matrix [this])
  (resize [this width height]))

(defn- -start
  [this]
  (println "starting projection")
  (assoc this :projection-matrix (Matrix4f.)))

(defn -stop [this] (println "stopping projection") this)

(defn- -get-projection-matrix [{:keys [projection-matrix]}] projection-matrix)

(defn -resize
  [{:keys [^Matrix4f projection-matrix]} width height]
  (let [{:keys [^Float fov ^Float z-near ^Float z-far]} config/config]
    (.identity projection-matrix)
    (.perspective projection-matrix
                  fov
                  ^Float (/ (float width) (float height))
                  z-near
                  z-far
                  true)))

(defrecord Projection []
  ProjectionI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (get-projection-matrix [this] (-get-projection-matrix this))
    (resize [this width height] (-resize this width height)))
