(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.instance
  (:require [clojure.set :as set]
            [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.vulkan-utils
             :as vulkan-utils]
            [nimaeskandary.vulkan-tutorial.chapter-6.utils :as utils])
  (:import (org.lwjgl.glfw GLFWVulkan)
           (org.lwjgl.system MemoryStack MemoryUtil)
           (org.lwjgl.vulkan EXTDebugUtils VkApplicationInfo)
           (org.lwjgl.vulkan KHRPortabilitySubset
                             VK12
                             VkDebugUtilsMessengerCallbackDataEXT
                             VkDebugUtilsMessengerCallbackEXTI
                             VkDebugUtilsMessengerCreateInfoEXT
                             VkInstance
                             VkLayerProperties
                             VkExtensionProperties
                             VkInstanceCreateInfo
                             KHRPortabilityEnumeration)))

(defprotocol InstanceI
  (start [this])
  (stop [this])
  (get-vk-instance [this]))

(def bitmask:message-severity
  (bit-or EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT
          EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT))
(def bitmask:message-type
  (bit-or EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_TYPE_GENERAL_BIT_EXT
          EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT
          EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_TYPE_PERFORMANCE_BIT_EXT))
(def ext:portability "VK_KHR_portability_enumeration")

(defn get-supported-validation-layers
  []
  (with-open [stack (MemoryStack/stackPush)]
    (let [num-layers-buff (.callocInt stack 1)
          _ (VK12/vkEnumerateInstanceLayerProperties num-layers-buff nil)
          num-layers (.get num-layers-buff 0)
          _ (println (format "instance supports %d layers" num-layers))
          props-buff (VkLayerProperties/calloc num-layers stack)
          _ (VK12/vkEnumerateInstanceLayerProperties num-layers-buff props-buff)
          supported-layers (set (for [^Integer i (range num-layers)]
                                  (let [layer-name (-> props-buff
                                                       ^VkLayerProperties
                                                       (.get i)
                                                       .layerNameString)]
                                    ;;(println (format "supports layer %s"
                                    ;;layer-name))
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
                                      "VK_LAYER_GOOGLE_unique_objects"})))))

(defn get-instance-exts
  []
  (with-open [stack (MemoryStack/stackPush)]
    (let [num-exts-buff (.callocInt stack 1)
          _ (VK12/vkEnumerateInstanceExtensionProperties ^String utils/nil*
                                                         num-exts-buff
                                                         nil)
          num-exts (.get num-exts-buff 0)
          _ (println (format "instance supports %d extensions" num-exts))
          props-buff (VkExtensionProperties/calloc num-exts stack)
          _ (VK12/vkEnumerateInstanceExtensionProperties ^String utils/nil*
                                                         num-exts-buff
                                                         props-buff)]
      (set (for [^Integer i (range num-exts)]
             (let [ext-name (-> props-buff
                                ^VkExtensionProperties (.get i)
                                .extensionNameString)]
               ;;(println (format "supports extension %s" ext-name))
               ext-name))))))

(defn create-debug-callback
  ^VkDebugUtilsMessengerCreateInfoEXT []
  (->
    (VkDebugUtilsMessengerCreateInfoEXT/calloc)
    (.sType
     EXTDebugUtils/VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT)
    (.messageSeverity bitmask:message-severity)
    (.messageType bitmask:message-type)
    (.pfnUserCallback
     (reify
      VkDebugUtilsMessengerCallbackEXTI
        (invoke [_ message-severity _message-types p-callback-data _p-user-date]
          (let [callback-data (VkDebugUtilsMessengerCallbackDataEXT/create
                               p-callback-data)]
            (cond
              (not=
               0
               (bit-and
                message-severity
                EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_INFO_BIT_EXT))
              (println (format "info - VkDebugUtilsCallback %s"
                               (.pMessageString callback-data)))
              (not=
               0
               (bit-and
                message-severity
                EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT))
              (println (format "warn - VkDebugUtilsCallback %s"
                               (.pMessageString callback-data)))
              (not=
               0
               (bit-and
                message-severity
                EXTDebugUtils/VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT))
              (println (format "error - VkDebugUtilsCallback %s"
                               (.pMessageString callback-data)))
              :else (println (format "debug - VkDebugUtilsCallback %s"
                                     (.pMessageString callback-data))))
            (VK12/VK_FALSE)))))))

(defn -start
  [{:keys [validate?], :as this}]
  (println "starting vulkan instance")
  (with-open [stack (MemoryStack/stackPush)]
    (let
      [;; create application info
       app-short-name (.UTF8 stack "VulkanBook")
       app-info (-> stack
                    VkApplicationInfo/calloc
                    (.sType VK12/VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    (.pApplicationName app-short-name)
                    (.applicationVersion 1)
                    (.pEngineName app-short-name)
                    (.engineVersion 0)
                    (.apiVersion VK12/VK_API_VERSION_1_2))
       ;; validation layers
       validation-layers (get-supported-validation-layers)
       num-validation-layers (count validation-layers)
       _ (when (and validate? (= 0 num-validation-layers))
           (println
            "requested validation but no supported validation layers found"))
       validate? (and validate? (pos? num-validation-layers))
       ;; required layers
       required-layers
       (when validate?
         (let [rlp (.mallocPointer stack num-validation-layers)]
           (doseq [^Integer i (range num-validation-layers)]
             (println (format "using validation layer %s"
                              (.get validation-layers i)))
             (.put rlp i (.ASCII stack (.get validation-layers i))))
           rlp))
       ;; required extensions
       instance-exts (get-instance-exts)
       glfw-exts (GLFWVulkan/glfwGetRequiredInstanceExtensions)
       _ (when (nil? glfw-exts)
           (throw (Exception.
                   "Failed to find the GLFW platform surface extensions")))
       use-portability? (and (instance-exts ext:portability)
                             (= vulkan-utils/os-type:macos
                                vulkan-utils/os-type))
       required-exts
       (if validate?
         (let [vk-debug-utils-ext
               (.UTF8 stack EXTDebugUtils/VK_EXT_DEBUG_UTILS_EXTENSION_NAME)
               num-extensions (if use-portability?
                                (-> glfw-exts
                                    .remaining
                                    (+ 2))
                                (-> glfw-exts
                                    .remaining
                                    inc))
               required-exts (.mallocPointer stack num-extensions)]
           (.put required-exts glfw-exts)
           (.put required-exts vk-debug-utils-ext)
           (when use-portability?
             (.put required-exts (.UTF8 stack ext:portability)))
           (.flip required-exts))
         (let [num-extensions (if use-portability?
                                (-> glfw-exts
                                    .remaining
                                    inc)
                                (.remaining glfw-exts))
               required-exts (.mallocPointer stack num-extensions)]
           (.put required-exts glfw-exts)
           (when use-portability?
             (.put
              required-exts
              (.UTF8
               stack
               KHRPortabilitySubset/VK_KHR_PORTABILITY_SUBSET_EXTENSION_NAME)))
           (.flip required-exts)))
       debug-utils (when validate? (create-debug-callback))
       ext (if debug-utils (.address debug-utils) (MemoryUtil/NULL))
       ;; create instance info
       instance-info (doto (VkInstanceCreateInfo/calloc stack)
                       (.sType VK12/VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                       (.pNext ext)
                       (.pApplicationInfo app-info)
                       (.ppEnabledLayerNames required-layers)
                       (.ppEnabledExtensionNames required-exts))
       _
       (when use-portability?
         (.flags
          instance-info
          KHRPortabilityEnumeration/VK_INSTANCE_CREATE_ENUMERATE_PORTABILITY_BIT_KHR))
       p-instance (.mallocPointer stack 1)
       _ (vulkan-utils/vk-check
          (VK12/vkCreateInstance instance-info nil p-instance)
          "error creating instance")
       vk-instance (VkInstance. (.get p-instance 0) instance-info)
       vk-debug-handle (if validate?
                         (let [b-long (.mallocLong stack 1)]
                           (vulkan-utils/vk-check
                            (EXTDebugUtils/vkCreateDebugUtilsMessengerEXT
                             vk-instance
                             debug-utils
                             nil
                             b-long)
                            "error creating debug utils")
                           (.get b-long 0))
                         VK12/VK_NULL_HANDLE)]
      (assoc this
             :vk-instance vk-instance
             :debug-utils debug-utils
             :vk-debug-handle vk-debug-handle))))

(defn -stop
  [{:keys [vk-instance vk-debug-handle debug-utils], :as this}]
  (println "stopping vulkan instance")
  (when (not= VK12/VK_NULL_HANDLE vk-debug-handle)
    (EXTDebugUtils/vkDestroyDebugUtilsMessengerEXT vk-instance
                                                   vk-debug-handle
                                                   nil))
  (VK12/vkDestroyInstance vk-instance nil)
  (when debug-utils
    (-> debug-utils
        .pfnUserCallback
        .free))
  this)

(defn -get-vk-instance [this] (:vk-instance this))

(defrecord Instance [validate?]
  InstanceI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (get-vk-instance [this] (-get-vk-instance this)))
