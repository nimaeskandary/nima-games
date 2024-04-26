(ns nimaeskandary.vulkan-tutorial.chapter-6.app-logic
  (:require [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.render :as
             graph.render]
            [nimaeskandary.vulkan-tutorial.chapter-6.eng.scene.mesh-data :as
             scene.mesh-data]))

(defprotocol AppLogicI
  (start [this])
  (stop [this])
  (input [this window scene diff-time-millis])
  (init [this window scene render])
  (update-fn [this window scene diff-time-millis]))

(defn- -init
  [_ _window _scene render]
  (let [model-id "TriangleModel"
        mesh-data (scene.mesh-data/->MeshData
                   (float-array [-0.5 -0.5 0.0 0.0 0.5 0.0 0.5 -0.5 0.0])
                   (int-array [0 1 2]))
        mesh-data-list [mesh-data]
        model-data (scene.mesh-data/->ModelData model-id mesh-data-list)
        model-data-list [model-data]]
    (graph.render/load-models render model-data-list)))

(defrecord AppLogic []
  AppLogicI
    (start [this] this)
    (stop [this] this)
    (input [_this _window _scene _diff-time-millis])
    (init [this window scene render] (-init this window scene render))
    (update-fn [_this _window _scene _diff-time-millis]))
