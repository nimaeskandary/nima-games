(ns nimaeskandary.vulkan-tutorial.chapter-4.eng.mouse-input
  (:require [nimaeskandary.vulkan-tutorial.chapter-4.eng.proto.mouse-input :as
             proto.mouse-input])
  (:import (org.joml Vector2f)
           (org.lwjgl.glfw GLFW
                           GLFWCursorEnterCallbackI
                           GLFWCursorPosCallbackI
                           GLFWMouseButtonCallbackI)))

(defn start
  [this window-handle]
  (println "starting mouse input")
  (let [previous-pos (Vector2f. (float -1) (float -1))
        current-pos (Vector2f.)
        displ-vec (Vector2f.)
        left-button-pressed? (atom false)
        right-button-pressed? (atom false)
        in-window? (atom false)
        _ (do (GLFW/glfwSetCursorPosCallback
               window-handle
               (reify
                GLFWCursorPosCallbackI
                  (invoke [_ _handle xpos ypos]
                    (.set current-pos (float xpos) (float ypos)))))
              (GLFW/glfwSetCursorEnterCallback
               window-handle
               (reify
                GLFWCursorEnterCallbackI
                  (invoke [_ _handle entered?] (reset! in-window? entered?))))
              (GLFW/glfwSetMouseButtonCallback
               window-handle
               (reify
                GLFWMouseButtonCallbackI
                  (invoke [_ _handle button action _mode]
                    (reset! left-button-pressed?
                      (and (= GLFW/GLFW_MOUSE_BUTTON_1 button)
                           (= GLFW/GLFW_PRESS action)))
                    (reset! right-button-pressed?
                      (and (= GLFW/GLFW_MOUSE_BUTTON_2 button)
                           (= GLFW/GLFW_PRESS action)))))))]
    (assoc this
           :previous-pos previous-pos
           :current-pos current-pos
           :displ-vec displ-vec
           :left-button-pressed? left-button-pressed?
           :right-button-pressed? right-button-pressed?
           :in-window? in-window?)))

(defn stop [_])

(defn get-current-pos [this] (:current-pos this))

(defn get-displ-vec [this] (:displ-vec this))

(defn input
  [{:keys [previous-pos current-pos displ-vec in-window?]}]
  (.set displ-vec (float 0) (float 0))
  (when (and (> (.x previous-pos) 0) (> (.y previous-pos) 0) @in-window?)
    (let [delta-x (- (.x current-pos) (.x previous-pos))
          delta-y (- (.y current-pos) (.y previous-pos))
          rotate-x? (not= delta-x 0)
          rotate-y? (not= delta-y 0)]
      ;; unsure why these are flipped in the example
      (when rotate-x? (set! (.-y displ-vec) (float delta-x)))
      (when rotate-y? (set! (.-x displ-vec) (float delta-y))))
    (.set previous-pos (.x current-pos) (.y current-pos))))

(defn is-left-button-pressed? [this] (deref (:left-button-pressed? this)))

(defn is-rigth-button-pressed? [this] (deref (:right-button-pressed? this)))

(defrecord MouseInput [window-handle]
  proto.mouse-input/MouseInput
    (start [this] (start this window-handle))
    (stop [this] (stop this))
    (get-current-pos [this] (get-current-pos this))
    (get-displ-vec [this] (get-displ-vec this))
    (input [this] (input this))
    (is-left-button-pressed? [this] (is-left-button-pressed? this))
    (is-right-button-pressed? [this] (is-rigth-button-pressed? this)))
