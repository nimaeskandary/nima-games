(ns nimaeskandary.super-kaolio.interface.start-dev
  (:require [nimaeskandary.super-kaolio.interface.start :as start]
            [nimaeskandary.super-kaolio.interface.core :as c]
            [clojure.spec.test.alpha :as st]
            [clojure.spec.alpha :as s]
            [play-cljc.gl.core :as pc]
            [nrepl.server :refer [start-server stop-server]])
  (:import [org.lwjgl.glfw GLFW]
           [nimaeskandary.super_kaolio.interface.start Window]))

(defn start
  []
  ;;(start-server :port 7888)
  (st/instrument)
  (st/unstrument 'odoyle.rules/insert) ;; don't require specs for
                                       ;; attributes
  (let [window (start/->window)
        game (pc/->game (:handle window))]
    (start/start game window)))

(defn -main [] (start))
