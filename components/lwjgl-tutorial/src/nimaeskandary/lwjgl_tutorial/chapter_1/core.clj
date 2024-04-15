(ns nimaeskandary.lwjgl-tutorial.chapter-1.core
  (:import (org.lwjgl.glfw GLFWErrorCallback GLFW Callbacks GLFWKeyCallbackI)
           (org.lwjgl.opengl GL GL11)
           (org.lwjgl.system MemoryStack)))

;; https://ahbejarano.gitbook.io/lwjglgamedev/chapter-01

(defn init
  []
  ;; error callback to system.err
  (-> System/err
      GLFWErrorCallback/createPrint
      .set)
  ;; init GLFW
  (when (not (GLFW/glfwInit)) (throw (Exception. "unable to start GLFW")))
  ;; configure GLFW
  (GLFW/glfwDefaultWindowHints)
  (GLFW/glfwWindowHint GLFW/GLFW_VISIBLE GLFW/GLFW_FALSE)
  (GLFW/glfwWindowHint GLFW/GLFW_RESIZABLE GLFW/GLFW_TRUE)
  ;; create window
  (let [window (GLFW/glfwCreateWindow 300 300 "Hello World!" 0 0)]
    (when (not window) (throw (Exception. "unable to create GLFW window")))
    ;; setup a key callback
    (GLFW/glfwSetKeyCallback window
                             (reify
                              GLFWKeyCallbackI
                                (invoke [_ w key _scancode action _mods]
                                  (when (and (= GLFW/GLFW_KEY_ESCAPE key)
                                             (= GLFW/GLFW_RELEASE action))
                                    (GLFW/glfwSetWindowShouldClose w true)))))
    ;; get thread stack and push new frame
    (let [stack (MemoryStack/stackPush)
          pWidth (.mallocInt stack 1)
          pHeight (.mallocInt stack 1)
          _ (GLFW/glfwGetWindowSize window pWidth pHeight)
          vidmode (GLFW/glfwGetVideoMode (GLFW/glfwGetPrimaryMonitor))
          centerX (/ (- (.width vidmode) (.get pWidth 0)) 2)
          centerY (/ (- (.width vidmode) (.get pWidth 0)) 2)]
      (GLFW/glfwSetWindowPos window centerX centerY))
    ;; make OpenGL context current
    (GLFW/glfwMakeContextCurrent window)
    ;; enable v sync
    (GLFW/glfwSwapInterval 1)
    (GLFW/glfwShowWindow window)
    (GL/createCapabilities)
    window))

(defn main-loop
  [window]
  (GL11/glClearColor 1.0 0.0 1.0 1.0)
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))
  (GLFW/glfwSwapBuffers window)
  (GLFW/glfwPollEvents))

(defn run
  []
  (let [window (init)]
    (while (not (GLFW/glfwWindowShouldClose window)) (main-loop window))
    (Callbacks/glfwFreeCallbacks window)
    (GLFW/glfwDestroyWindow window)
    (GLFW/glfwTerminate)
    (-> (GLFW/glfwSetErrorCallback nil)
        .free)))

(defn -main [] (run))
