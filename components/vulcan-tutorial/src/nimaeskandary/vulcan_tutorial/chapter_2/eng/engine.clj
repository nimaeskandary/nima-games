(ns nimaeskandary.vulcan-tutorial.chapter-2.eng.engine
  (:require
    [nimaeskandary.vulcan-tutorial.chapter-2.eng.graph.render :as graph.render]
    [nimaeskandary.vulcan-tutorial.chapter-2.eng.scene.scene :as scene.scene]
    [nimaeskandary.vulcan-tutorial.chapter-2.eng.window :as eng.window]
    [nimaeskandary.vulcan-tutorial.chapter-2.eng.proto.engine :as proto.engine]
    [nimaeskandary.vulcan-tutorial.chapter-2.eng.proto.window :as proto.window]
    [nimaeskandary.vulcan-tutorial.chapter-2.eng.proto.app-logic :as
     proto.app-logic]
    [nimaeskandary.vulcan-tutorial.chapter-2.eng.proto.render :as
     proto.render]))

;; not using
;; https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-01/src/main/java/org/vulkanb/eng/EngineProperties.java
(def default-ups 30)

(defn start
  [this window-title app-logic]
  (let [window (proto.window/start (eng.window/->Window window-title nil))
        scene (scene.scene/->Scene window)
        render (proto.render/start (graph.render/->Render window scene))]
    (proto.app-logic/init app-logic window scene render)
    (assoc this
           :app-logic app-logic
           :window window
           :scene scene
           :render render
           :running? (atom false))))

(defn stop
  [{:keys [app-logic render window]}]
  (proto.app-logic/stop app-logic)
  (proto.render/stop render)
  (proto.window/stop window))

(defn run-loop
  [window app-logic render scene time-u init-time-atom delta-update-atom
   update-time-atom]
  (proto.window/poll-events window)
  (let [current-time (System/currentTimeMillis)]
    (swap! delta-update-atom + (/ (- current-time @init-time-atom) time-u))
    (proto.app-logic/input app-logic
                           window
                           scene
                           (- current-time @init-time-atom))
    (when (>= @delta-update-atom 1)
      (let [diff-time-millis (- current-time @update-time-atom)]
        (proto.app-logic/update-fn app-logic window scene diff-time-millis)
        (reset! update-time-atom current-time)
        (swap! delta-update-atom dec)))
    (proto.render/render render window scene)
    (reset! init-time-atom current-time)))

(defn run
  [{:keys [running? window app-logic render scene], :as this}]
  (let [init-time-atom (atom (System/currentTimeMillis))
        delta-update-atom (atom 0)
        update-time-atom (atom @init-time-atom)
        time-u (/ (float 1000) default-ups)]
    (while (and @running? (not (proto.window/should-close? window)))
      (run-loop window
                app-logic
                render
                scene
                time-u
                init-time-atom
                delta-update-atom
                update-time-atom))
    (proto.engine/stop this)))

(defn init [this] (reset! (:running? this) true) (proto.engine/run this))

(defrecord Engine [window-title app-logic]
  proto.engine/Engine
    (start [this] (start this window-title app-logic))
    (stop [this] (stop this))
    (run [this] (run this))
    (init [this] (init this)))
