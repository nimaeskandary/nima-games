(ns nimaeskandary.super-kaolio.interface.move
  (:require [play-cljc.instances :as i]
            [nimaeskandary.super-kaolio.interface.utils :as utils]
            [nimaeskandary.super-kaolio.interface.tiles :as tiles]
            #?(:clj [play-cljc.macros-java :refer [gl math]]
               :cljs [play-cljc.macros-js :refer-macros [gl math]])))

(def damping 0.1)
(def max-velocity 16)
(def max-jump-velocity (* max-velocity 10))
(def max-movement-per-frame 0.5)
(def min-movement-per-frame -0.5)
(def deceleration 0.8)
(def gravity 3)
(def animation-secs 0.2)

(defn decelerate
  [velocity]
  (let [velocity (* velocity deceleration)]
    (if (< (math abs velocity) damping) 0 velocity)))

(defn get-x-velocity
  [{:keys [pressed-keys x-velocity]}]
  (cond (contains? pressed-keys :left) (* -1 max-velocity)
        (contains? pressed-keys :right) max-velocity
        :else x-velocity))

(defn get-y-velocity
  [{:keys [pressed-keys y-velocity can-jump?]}]
  (cond (and can-jump? (contains? pressed-keys :up)) (* -1 max-jump-velocity)
        :else y-velocity))

(defn get-direction
  [{:keys [x-velocity direction]}]
  (cond (> x-velocity 0) :right
        (< x-velocity 0) :left
        :else direction))

(defn move
  [{:keys [delta-time], :as game}
   {:keys [player-x player-y can-jump?], :as state}]
  (let [x-velocity (get-x-velocity state)
        y-velocity (+ (get-y-velocity state) gravity)
        x-change (-> (* x-velocity delta-time)
                     (max min-movement-per-frame)
                     (min max-movement-per-frame))
        y-change (-> (* y-velocity delta-time)
                     (max min-movement-per-frame)
                     (min max-movement-per-frame))]
    (if (or (not= 0 x-change) (not= 0 y-change))
      (assoc state
             :x-velocity (decelerate x-velocity)
             :y-velocity (decelerate y-velocity)
             :x-change x-change
             :y-change y-change
             :player-x (+ player-x x-change)
             :player-y (+ player-y y-change)
             :can-jump? (if (neg? y-velocity) false can-jump?))
      state)))

(defn prevent-move
  [{:keys [player-x player-y player-width player-height x-change y-change
           tiled-map tiled-map-entity y-velocity],
    :as state}]
  (let [old-x (- player-x x-change)
        old-y (- player-y y-change)
        up? (neg? y-change)
        horiz-tile (tiles/touching-tile tiled-map
                                        "walls"
                                        player-x
                                        old-y
                                        player-width
                                        player-height)
        vert-tile (tiles/touching-tile tiled-map
                                       "walls"
                                       old-x
                                       player-y
                                       player-width
                                       player-height)]
    (cond-> state
      horiz-tile (assoc :x-velocity 0 :x-change 0 :player-x old-x)
      vert-tile
      (assoc :y-velocity 0 :y-change 0 :player-y old-y :can-jump? (not up?))
      ;; if we are going up, destroy whatever tile we hit
      (and vert-tile (neg? y-velocity))
      (as-> state (let [{:keys [layer tile-x tile-y]} vert-tile
                        tile-id (.indexOf (:tiles tiled-map) vert-tile)]
                    (assoc state
                           :tiled-map-entity (i/dissoc tiled-map-entity tile-id)
                           :tiled-map
                           (-> tiled-map
                               (update :tiles
                                       (fn [tiles]
                                         (-> (subvec tiles 0 tile-id)
                                             (into (subvec tiles
                                                           (inc tile-id))))))
                               (assoc-in [:layers layer tile-x tile-y]
                                         nil))))))))

(defn animate
  [{:keys [total-time]}
   {:keys [x-velocity y-velocity direction player-images player-walk-keys],
    :as state}]
  (let [direction (get-direction state)]
    (-> state
        (assoc :player-image-key
               (cond (not= y-velocity 0) :jump
                     (not= x-velocity 0)
                     (let [cycle-time (mod total-time
                                           (* animation-secs
                                              (count player-walk-keys)))]
                       (nth player-walk-keys
                            (int (/ cycle-time animation-secs))))
                     :else :stand))
        (assoc :direction direction))))
