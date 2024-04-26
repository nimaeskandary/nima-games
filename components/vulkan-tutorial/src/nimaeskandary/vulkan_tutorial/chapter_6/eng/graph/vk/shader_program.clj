(ns nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.shader-program
  (:require [clojure.java.io :as io]
            [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.device :as
             vk.device]
            [nimaeskandary.vulkan-tutorial.chapter-6.eng.graph.vk.vulkan-utils
             :as vulkan-utils])
  (:import (java.io File IOException)
           (java.nio.file Files)
           (org.lwjgl.system MemoryStack)
           (org.lwjgl.util.shaderc Shaderc)
           (org.lwjgl.vulkan VK12 VkDevice VkShaderModuleCreateInfo)))

(defprotocol ShaderProgramI
  (start [this])
  (stop [this])
  (get-shader-modules [this]))

(defrecord ShaderModule [^Integer shader-stage ^Long handle])
(defrecord ShaderModuleData [^Integer shader-stage ^String shader-spv-file])

(defn- create-shader-module
  [device ^bytes code]
  (with-open [stack (MemoryStack/stackPush)]
    (let [p-code (-> (.malloc stack (alength code))
                     (.put 0 code))
          module-create-info (-> (VkShaderModuleCreateInfo/calloc stack)
                                 .sType$Default
                                 (.pCode p-code))
          long-b (.mallocLong stack 1)]
      (-> (VK12/vkCreateShaderModule ^VkDevice (vk.device/get-vk-device device)
                                     module-create-info
                                     nil
                                     long-b)
          (vulkan-utils/vk-check "failed to create shader module"))
      (.get long-b 0))))

(defn- -start
  [{:keys [device shader-module-data-list], :as this}]
  (println "starting shader program")
  (try
    (let [shader-modules
          (->> shader-module-data-list
               (map (fn [{:keys [^Integer shader-stage
                                 ^String shader-spv-file]}]
                      (let [module-contents (-> shader-spv-file
                                                ^File io/as-file
                                                .toPath
                                                Files/readAllBytes)
                            module-handle
                            (create-shader-module device module-contents)]
                        (->ShaderModule shader-stage module-handle))))
               doall)]
      (assoc this :shader-modules shader-modules))
    (catch IOException e (println "error reading shader files" e) (throw e))))

(defn- -stop
  [{:keys [shader-modules device], :as this}]
  (println "stopping shader program")
  (doseq [shader-module shader-modules]
    (VK12/vkDestroyShaderModule (vk.device/get-vk-device device)
                                (:handle shader-module)
                                nil))
  this)

(defn- -get-shader-modules [{:keys [shader-modules]}] shader-modules)

(defrecord ShaderProgram [device shader-module-data-list]
  ShaderProgramI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (get-shader-modules [this] (-get-shader-modules this)))

(defn compile-shader
  ^bytes [^String shader-code ^Integer shader-type]
  (println "compiling shader...")
  (let [compiler (atom nil)
        options (atom nil)]
    (try (reset! compiler (Shaderc/shaderc_compiler_initialize))
         (reset! options (Shaderc/shaderc_compile_options_initialize))
         (let [^Long compiler @compiler
               ^Long options @options
               result (Shaderc/shaderc_compile_into_spv compiler
                                                        shader-code
                                                        shader-type
                                                        "shader.glsl"
                                                        "main"
                                                        options)
               _ (when (not= Shaderc/shaderc_compilation_status_success
                             (Shaderc/shaderc_result_get_compilation_status
                              result))
                   (throw (Exception. ^String
                                      (str
                                       "shader compilation failed: "
                                       (Shaderc/shaderc_result_get_error_message
                                        result)))))
               buffer (Shaderc/shaderc_result_get_bytes result)
               compiled-shader (byte-array (.remaining buffer))]
           (.get buffer compiled-shader)
           compiled-shader)
         (finally
          (when @options (Shaderc/shaderc_compile_options_release @options))
          (when @compiler (Shaderc/shaderc_compiler_release @compiler))))))

(defn compile-shader-if-changed
  [^String gls-shader-file-path ^String spv-shader-file-path
   ^Integer shader-type]
  (try (let [glsl-resource (io/resource gls-shader-file-path)
             spv-output (io/file spv-shader-file-path)]
         (if (or (not spv-output)
                 (> (.lastModified ^File (io/as-file glsl-resource))
                    (.lastModified spv-output)))
           (do (println (format "compiling %s to %s"
                                glsl-resource
                                spv-shader-file-path))
               (let [shader-code (slurp glsl-resource)
                     compiled-shader (compile-shader shader-code shader-type)]
                 (println "done compiling")
                 (io/copy compiled-shader spv-output)))
           (println (format "%s already exists, loading compiled version"
                            spv-output))))
       (catch IOException e (throw e))))
