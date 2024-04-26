(ns nimaeskandary.vulkan-tutorial.chapter-7.eng.config
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def config
  (-> "vulkan-tutorial/config.edn"
      io/resource
      slurp
      edn/read-string))
