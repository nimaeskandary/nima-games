(ns nimaeskandary.vulkan-tutorial.chapter-8.eng.scene.mesh-data)

(defrecord ModelData [^String model-id mesh-data-list])

(defrecord MeshData [^floats positions ^floats text-coords ^ints indices])
