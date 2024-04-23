(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.vulkan-utils
  (:require [clojure.string :as str])
  (:import (java.util Locale)
           (org.lwjgl.vulkan VK12)))

(def os-type:macos "macos")
(def os-type:windows "windows")
(def os-type:linux "linux")
(def os-type:other "other")

(def os-name
  (-> (System/getProperty "os.name" "generic")
      (.toLowerCase Locale/ENGLISH)))

(def os-type
  (cond (or (str/includes? os-name "mac") (str/includes? os-name "darwin"))
        os-type:macos
        (str/includes? os-name "win") os-type:windows
        (str/includes? os-name "nux") os-type:linux
        :else os-type:other))

(defn vk-check
  [error-code error-message]
  (when (not= VK12/VK_SUCCESS error-code)
    (throw (Exception. (format "%d:%s" error-message error-code)))))
