(ns nimaeskandary.vulkan-tutorial.chapter-4.eng.window
  (:require
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.proto.window :as proto.window]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.proto.mouse-input :as
     proto.mouse-input]
    [nimaeskandary.vulkan-tutorial.chapter-4.eng.mouse-input :as mouse-input])
  (:import (org.lwjgl.glfw Callbacks
                           GLFW
                           GLFWFramebufferSizeCallbackI
                           GLFWKeyCallbackI
                           GLFWVulkan)
           (org.lwjgl.system MemoryUtil)))

(defn start
  [{:keys [^String title ^GLFWKeyCallbackI key-callback], :as this}]
  (println "starting window")
  (when (not (GLFW/glfwInit)) (throw (Exception. "unable to start GLFW")))
  (when (not (GLFWVulkan/glfwVulkanSupported))
    (throw (Exception.
            "Cannot find a compatible Vulkan installable client driver (ICD)")))
  (let [vidmode (GLFW/glfwGetVideoMode (GLFW/glfwGetPrimaryMonitor))
        width (.width vidmode)
        height (.height vidmode)
        _ (do (GLFW/glfwDefaultWindowHints)
              (GLFW/glfwWindowHint GLFW/GLFW_CLIENT_API GLFW/GLFW_NO_API)
              (GLFW/glfwWindowHint GLFW/GLFW_MAXIMIZED GLFW/GLFW_FALSE))
        window-handle (GLFW/glfwCreateWindow width
                                             height
                                             title
                                             (MemoryUtil/NULL)
                                             (MemoryUtil/NULL))
        this (assoc this
                    :window-handle window-handle
                    :height (atom height)
                    :width (atom width)
                    :resized? (atom false))
        _ (do (when (= (MemoryUtil/NULL) window-handle)
                (throw (Exception. "Failed to create the GLFW window")))
              (GLFW/glfwSetFramebufferSizeCallback
               window-handle
               (reify
                GLFWFramebufferSizeCallbackI
                  (invoke [_ _ w h] (proto.window/resize this w h))))
              (GLFW/glfwSetKeyCallback
               window-handle
               (reify
                GLFWKeyCallbackI
                  (invoke [_ window-handle_ key scancode action mods]
                    (when (and (= GLFW/GLFW_KEY_ESCAPE key)
                               (= GLFW/GLFW_RELEASE action))
                      (GLFW/glfwSetWindowShouldClose window-handle_ true))
                    (when key-callback
                      (.invoke key-callback
                               window-handle_
                               key
                               scancode
                               action
                               mods))))))
        mouse-input (proto.mouse-input/start (mouse-input/->MouseInput
                                              window-handle))]
    (assoc this :mouse-input mouse-input)))


(defn stop
  [{:keys [window-handle], :as this}]
  (println "stopping window")
  (Callbacks/glfwFreeCallbacks window-handle)
  (GLFW/glfwDestroyWindow window-handle)
  (GLFW/glfwTerminate)
  this)

(defn get-height [this] (deref (:height this)))

(defn get-width [this] (deref (:width this)))

(defn get-mouse-input [this] (:mouse-input this))

(defn get-window-handle [this] (:window-handle this))

(defn is-key-pressed?
  [this key-code]
  (= GLFW/GLFW_PRESS (GLFW/glfwGetKey (:window-handle this) key-code)))

(defn is-resized? [this] (deref (:resized? this)))

(defn poll-events
  [this]
  (GLFW/glfwPollEvents)
  (proto.mouse-input/input (:mouse-input this)))

(defn reset-resized [this] (reset! (:resized? this) false))

(defn resize
  [{:keys [resized? width height]} w h]
  (reset! resized? true)
  (reset! width w)
  (reset! height h))

(defn set-resized [this value] (reset! (:resized? this) value))

(defn set-should-close
  [this]
  (GLFW/glfwSetWindowShouldClose (:window-handle this) true))

(defn should-close? [this] (GLFW/glfwWindowShouldClose (:window-handle this)))

(defrecord Window [title key-callback]
  proto.window/Window
    (start [this] (start this))
    (stop [this] (stop this))
    (get-height [this] (get-height this))
    (get-width [this] (get-width this))
    (get-mouse-input [this] (get-mouse-input this))
    (get-window-handle [this] (get-window-handle this))
    (is-key-pressed? [this key-code] (is-key-pressed? this key-code))
    (is-resized? [this] (is-resized? this))
    (poll-events [this] (poll-events this))
    (reset-resized [this] (reset-resized this))
    (resize [this width height] (resize this width height))
    (set-resized [this value] (set-resized this value))
    (set-should-close [this] (set-should-close this))
    (should-close? [this] (should-close? this)))
