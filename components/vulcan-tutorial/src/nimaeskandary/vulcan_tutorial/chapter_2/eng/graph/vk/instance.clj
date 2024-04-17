(ns nimaeskandary.vulcan-tutorial.chapter-2.eng.graph.vk.instance
  (:require [clojure.set :as set])
  (:import (org.lwjgl.system MemoryStack)
           [org.lwjgl.vulkan EXTDebugUtils VkApplicationInfo]
           (org.lwjgl.vulkan VK13 VkLayerProperties)))

(def message-security-bitmask
  (bit-or EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
          EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT))
(def message-type-bitmask
  (bit-or EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
          EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
          EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT))
(def portability-extension "VK_KHR_portability_enumeration")

(defn get-supported-validation-layers
  []
  (let [stack (MemoryStack/stackPush)
        num-layers-buff (.callocInt stack 1)
        _ (VK13/vkEnumerateInstanceLayerProperties num-layers-buff nil)
        num-layers (.get num-layers-buff 0)
        _ (println (format "instance supports %d layers" num-layers))
        props-buff (VkLayerProperties/calloc num-layers stack)
        _ (VK13/vkEnumerateInstanceLayerProperties num-layers-buff props-buff)
        supported-layers (set (for [i (range num-layers)]
                                (let [layer-name (-> props-buff
                                                     ^VkLayerProperties
                                                     (.get ^Integer i)
                                                     .layerNameString)]
                                  (println "supports layer %s" layer-name)
                                  layer-name)))]
    (cond (supported-layers "VK_LAYER_KHRONOS_validation")
          ["VK_LAYER_KHRONOS_validation"]
          (supported-layers "VK_LAYER_LUNARG_standard_validation")
          ["VK_LAYER_LUNARG_standard_validation"]
          :else (set/intersection supported-layers
                                  #{"VK_LAYER_GOOGLE_threading"
                                    "VK_LAYER_LUNARG_parameter_validation"
                                    "VK_LAYER_LUNARG_object_tracker"
                                    "VK_LAYER_LUNARG_core_validation"
                                    "VK_LAYER_GOOGLE_unique_objects"}))))

(defn start
  [this validate?]
  (println "creating vulkan instance")
  (let [stack (MemoryStack/stackPush)
        ;; create application info
        app-short-name (.UTF8 stack "VulkanBook")
        app-info (-> stack
                     VkApplicationInfo/calloc
                     (.sType VK13/VK_STRUCTURE_TYPE_APPLICATION_INFO)
                     (.pApplicationName app-short-name)
                     (.applicationVersion 1)
                     (.pEngineName app-short-name)
                     (.engineVersion 0)
                     (.apiVersion VK13/VK_API_VERSION_1_3))
        ;; validation layers
        validation-layers (get-supported-validation-layers)
        num-validation-layers (count validation-layers)
        _ (when (and validate? (= 0 num-validation-layers))
            (println
              "requested validation but no supported validation layers found"))
        validate? (and validate? (pos? num-validation-layers))]))

;; at
;; https://github.com/lwjglgamedev/vulkanbook/blob/master/booksamples/chapter-02/src/main/java/org/vulkanb/eng/graph/vk/Instance.java#L54
