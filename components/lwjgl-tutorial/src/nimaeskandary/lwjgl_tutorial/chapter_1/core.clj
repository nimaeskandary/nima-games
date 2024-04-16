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
  (let [window (GLFW/glfwCreateWindow 300
                                      300
                                      "Hello World!"
                                      0
                                      #_(GLFW/glfwGetPrimaryMonitor)
                                      0)]
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
    (let [;; need to use native accessible memory to interact with native
          ;; libraries
          stack (MemoryStack/stackPush)
          ;; allocate memory for two ints
          pWidth (.mallocInt stack 1)
          pHeight (.mallocInt stack 1)
          ;; set values for those ints via pass by reference
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
    ;; needed for interop with LWJGL and GLFW
    (GL/createCapabilities)
    window))

(defn main-loop
  [window]
  ;; set clear color
  (GL11/glClearColor 1.0 1.0 1.0 1.0)
  ;; clear frame buffer
  (GL11/glClear (bit-or GL11/GL_COLOR_BUFFER_BIT GL11/GL_DEPTH_BUFFER_BIT))
  ;; swap color buffers
  (GLFW/glfwSwapBuffers window)
  ;; poll for escape key pressed
  (GLFW/glfwPollEvents))

(defn clean-up
  [window]
  (Callbacks/glfwFreeCallbacks window)
  (GLFW/glfwDestroyWindow window)
  (GLFW/glfwTerminate)
  (-> (GLFW/glfwSetErrorCallback nil)
      .free))

(defn run
  []
  (let [window (init)]
    (while (not (GLFW/glfwWindowShouldClose window)) (main-loop window))
    (clean-up window))
  (System/exit 0))

(defn -main [] (run))
