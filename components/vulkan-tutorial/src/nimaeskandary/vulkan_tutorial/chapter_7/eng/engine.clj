(ns nimaeskandary.vulkan-tutorial.chapter-7.eng.engine
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.graph.render :as graph.render]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.scene.scene :as scene.scene]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.window :as eng.window]
    [nimaeskandary.vulkan-tutorial.chapter-7.eng.config :as config]
    [nimaeskandary.vulkan-tutorial.chapter-7.app-logic :as app-logic]))

(defprotocol EngineI
  (start [this])
  (stop [this])
  (run [this])
  (init [this]))

(defn -start
  [{:keys [window-title app-logic], :as this}]
  (println "starting engine")
  (let [window (eng.window/start (eng.window/->Window window-title nil))
        scene (scene.scene/->Scene window)
        render (graph.render/start (graph.render/->Render window scene))]
    (app-logic/init app-logic window scene render)
    (assoc this
           :app-logic app-logic
           :window window
           :scene scene
           :render render
           :running? (atom false))))

(defn -stop
  [{:keys [app-logic render window], :as this}]
  (println "stopping engine")
  (app-logic/stop app-logic)
  (graph.render/stop render)
  (eng.window/stop window)
  this)

(defn run-loop
  [window app-logic render scene time-u init-time-atom delta-update-atom
   update-time-atom]
  (eng.window/poll-events window)
  (let [current-time (System/currentTimeMillis)]
    (swap! delta-update-atom + (/ (- current-time @init-time-atom) time-u))
    (app-logic/input app-logic window scene (- current-time @init-time-atom))
    (when (>= @delta-update-atom 1)
      (let [diff-time-millis (- current-time @update-time-atom)]
        (app-logic/update-fn app-logic window scene diff-time-millis)
        (reset! update-time-atom current-time)
        (swap! delta-update-atom dec)))
    (graph.render/render render)
    (reset! init-time-atom current-time)))

(defn -run
  [{:keys [running? window app-logic render scene], :as this}]
  (let [init-time-atom (atom (System/currentTimeMillis))
        delta-update-atom (atom 0)
        update-time-atom (atom @init-time-atom)
        time-u (/ (float 1000) (:ups config/config))]
    (while (and @running? (not (eng.window/should-close? window)))
      (run-loop window
                app-logic
                render
                scene
                time-u
                init-time-atom
                delta-update-atom
                update-time-atom))
    (stop this)
    (println "all stopped")))

(defn -init [this] (reset! (:running? this) true) (run this))

(defrecord Engine [window-title app-logic]
  EngineI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (run [this] (-run this))
    (init [this] (-init this)))
