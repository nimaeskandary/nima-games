(ns nimaeskandary.vulcan-tutorial.chapter-2.eng.graph.render
  (:require [nimaeskandary.vulcan-tutorial.chapter-2.eng.proto.render :as
             proto.render]))

(defn start [this _ _] this)

(defn stop [_])

(defn render [_ _ _])

(defrecord Render [window scene]
  proto.render/Render
    (start [this] (start this window scene))
    (stop [this] (stop this))
    (render [this window_ scene_] (render this window_ scene_)))
