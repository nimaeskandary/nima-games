(ns nimaeskandary.lwjgl-tutorial.chapter-2.window
  (:import (com.stuartsierra.component Lifecycle)
           (org.lwjgl.glfw GLFWErrorCallback
                           GLFW
                           Callbacks
                           GLFWErrorCallbackI
                           GLFWFramebufferSizeCallbackI
                           GLFWKeyCallbackI)
           (org.lwjgl.opengl GL GL11)
           (org.lwjgl.system MemoryStack MemoryUtil)))

(defn get-dimensions
  [width height]
  (if (and width (pos? width) height (pos? height))
    {:width width, :height height}
    (do (GLFW/glfwWindowHint GLFW/GLFW_MAXIMIZED GLFW/GLFW_TRUE)
        (let [vidmode (GLFW/glfwGetVideoMode (GLFW/glfwGetPrimaryMonitor))]
          {:width (.width vidmode), :height (.height vidmode)}))))

(defn get-window-handle
  [^Integer width ^Integer height ^String title ^Long monitor ^Long share]
  (let [windowHandle (GLFW/glfwCreateWindow width height title monitor share)]
    (when (not windowHandle)
      (throw (Exception. "failed to create GLFW window")))
    windowHandle))

(def key-callback
  (reify
   GLFWKeyCallbackI
     (invoke [_ w key _scancode action _mods]
       (when (and (= GLFW/GLFW_KEY_ESCAPE key) (= GLFW/GLFW_RELEASE action))
         (GLFW/glfwSetWindowShouldClose w true)))))

(defn frame-buffer-size-callback
  [resize-fn]
  (reify
   GLFWFramebufferSizeCallbackI
     (invoke [_ _ *width *height] (resize-fn *width *height))))

(def error-callback
  (reify
   GLFWErrorCallbackI
     (invoke [_ errorCode msgPtr]
       (println (format "Error code %d, msg %s"
                        errorCode
                        (MemoryUtil/memUTF8 msgPtr))))))

(defrecord Window [title resize-fn options])
(extend-type Window
 Lifecycle
   (start [{:keys [title resize-fn],
            {:keys [compatible-profile fps height ups width], :as options}
            :options,
            :as this}]
     ;; init GLFW
     (when (not (GLFW/glfwInit)) (throw (Exception. "unable to start GLFW")))
     ;; configure GLFW
     (GLFW/glfwDefaultWindowHints)
     (GLFW/glfwWindowHint GLFW/GLFW_VISIBLE GLFW/GLFW_FALSE)
     (GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE GLFW/GLFW_TRUE)
     (if compatible-profile
       (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_PROFILE
                            GLFW/GLFW_OPENGL_COMPAT_PROFILE)
       (do (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_PROFILE
                                GLFW/GLFW_OPENGL_CORE_PROFILE)
           (GLFW/glfwWindowHint GLFW/GLFW_OPENGL_FORWARD_COMPAT
                                GLFW/GLFW_TRUE)))
     (let [{:keys [width height]} (get-dimensions width height)
           window-handle (get-window-handle width height title 0 0)]
       (GLFW/glfwSetFramebufferSizeCallback window-handle
                                            (frame-buffer-size-callback
                                             resize-fn))
       (GLFW/glfwSetErrorCallback error-callback)
       (GLFW/glfwSetKeyCallback window-handle key-callback)
       (if (and fps (pos? fps))
         (GLFW/glfwSwapInterval 0)
         (GLFW/glfwSwapInterval 1))
       (GLFW/glfwShowWindow window-handle)
       (let [width-buffer (int-array 1)
             height-buffer (int-array 1)]
         (GLFW/glfwGetFramebufferSize window-handle width-buffer height-buffer)
         (assoc this
                :window-handle window-handle
                :width (first width-buffer)
                :height (first height-buffer))))))
