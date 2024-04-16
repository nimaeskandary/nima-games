(ns nimaeskandary.lwjgl-tutorial.chapter-2.proto)

(defprotocol IAppLogic
  (cleanup [this])
  (init [this window scene render])
  (input [this window scene diffTimeMillis])
  (update [this window scene diffTimeMillis]))

(defprotocol IWindow)
