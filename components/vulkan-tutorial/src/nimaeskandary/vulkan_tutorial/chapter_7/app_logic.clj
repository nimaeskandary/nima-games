(ns nimaeskandary.vulkan-tutorial.chapter-7.app-logic
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.render :as graph.render]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.scene.entity :as scene.entity]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.scene.mesh-data :as
     scene.mesh-data]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.scene.scene :as scene.scene])
  (:import (org.joml Quaternionf Vector3f)))

(defprotocol AppLogicI
  (start [this])
  (stop [this])
  (input [this window scene diff-time-millis])
  (init [this window scene render])
  (update-fn [this window scene diff-time-millis]))

(defn- -start
  [this]
  (println "starting app logic")
  (assoc this
         :cube-entity-atom (atom nil)
         :angle-atom (atom 0.0)
         :rotating-angle (Vector3f. 1 1 1)))

(defn -stop [this] (println "stopping app logic") this)

(defn- -init
  [{:keys [cube-entity-atom]} _window scene render]
  (let [model-id "CubeModel"
        positions (float-array [-0.5 0.5 0.5 -0.5 -0.5 0.5 0.5 -0.5 0.5 0.5 0.5
                                0.5 -0.5 0.5 -0.5 0.5 0.5 -0.5 -0.5 -0.5 -0.5
                                0.5 -0.5 -0.5])
        text-coords (float-array [0.0 0.0 0.5 0.0 1.0 0.0 1.0 0.5 1.0 1.0 0.5
                                  1.0 0.0 1.0 0.0 0.5])
        indices (int-array [;; Front face
                            0 1 3 3 1 2
                            ;; Top Face
                            4 0 3 5 4 3
                            ;; Right face
                            3 2 7 5 3 7
                            ;; Left face
                            6 1 0 6 0 4
                            ;; Bottom face
                            2 1 6 2 6 7
                            ;; Back face
                            7 6 4 7 4 5])
        mesh-data-list
        [(scene.mesh-data/->MeshData positions text-coords indices)]
        model-data-list [(scene.mesh-data/->ModelData model-id mesh-data-list)]
        _ (graph.render/load-models render model-data-list)
        _ (reset! cube-entity-atom (-> (scene.entity/->Entity
                                        "CubeEntity"
                                        model-id
                                        (Vector3f. 0.0 0.0 0.0))
                                       scene.entity/start))]
    (scene.entity/set-position @cube-entity-atom 0 0 -2)
    (scene.scene/add-entity scene @cube-entity-atom)))

(defn- -update-fn
  [{:keys [cube-entity-atom angle-atom rotating-angle]} _window _scene
   _diff-time-millis]
  (swap! angle-atom + 1.0)
  (when (>= @angle-atom 360) (swap! angle-atom - 360))
  (-> ^Quaternionf (scene.entity/get-rotation @cube-entity-atom)
      .identity
      (.rotateAxis (Math/toRadians @angle-atom) rotating-angle))
  (scene.entity/update-model-matrix @cube-entity-atom))

(defrecord AppLogic []
  AppLogicI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (input [_this _window _scene _diff-time-millis])
    (init [this window scene render] (-init this window scene render))
    (update-fn [this window scene diff-time-millis]
      (-update-fn this window scene diff-time-millis)))
