(ns nimaeskandary.vulkan-tutorial.chapter-7.eng.scene.mesh-data)

(defrecord ModelData [^String model-id mesh-data-list])

(defrecord MeshData [^floats positions ^ints indices])
