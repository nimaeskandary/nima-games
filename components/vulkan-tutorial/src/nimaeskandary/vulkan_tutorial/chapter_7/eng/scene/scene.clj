(ns nimaeskandary.vulkan-tutorial.chapter-7.eng.scene.scene
  (:require [nimaeskandary.vulkan-tutorial.chapter-7.eng.scene.projection :as
             scene.projection]
            [nimaeskandary.vulkan-tutorial.chapter-7.eng.window :as eng.window]
            [nimaeskandary.vulkan-tutorial.chapter-7.eng.scene.entity :as
             scene.entity]))

(defprotocol SceneI
  (start [this])
  (stop [this])
  (add-entity [this entity])
  (get-entities-by-model-id [this model-id])
  (get-entities-map [this])
  (get-projection [this])
  (remove-all-entities [this])
  (remove-entity [this]))

(defn- -start
  [{:keys [window], :as this}]
  (println "starting scene")
  (let [projection (-> (scene.projection/->Projection)
                       scene.projection/start)]
    (scene.projection/resize projection
                             (eng.window/get-width window)
                             (eng.window/get-height window))
    (assoc this :entities-map-atom (atom {}) :projection projection)))

(defn- -stop [this] (println "stopping scene") this)

(defn- -add-entity
  [{:keys [entities-map-atom]} entity]
  (let [model-id (scene.entity/get-model-id entity)
        entities (get @entities-map-atom model-id)]
    (if (nil? entities)
      (swap! entities-map-atom assoc model-id [entity])
      (swap! entities-map-atom assoc model-id (conj entities entity)))))

(defn- -get-entities-by-model-id
  [{:keys [entities-map-atom]} model-id]
  (get @entities-map-atom model-id))

(defn- -get-entities-map [{:keys [entities-map-atom]}] @entities-map-atom)

(defn- -get-projection [{:keys [projection]}] projection)

(defn- -remove-all-entities
  [{:keys [entities-map-atom]}]
  (reset! entities-map-atom {}))

(defn- -remove-entity
  [entities-map-atom target-entity]
  (let [model-id (scene.entity/get-model-id target-entity)
        entities (get @entities-map-atom model-id)]
    (swap! entities-map-atom assoc
      model-id
      (some->> entities
               (remove (fn [current]
                         (= (scene.entity/get-id target-entity)
                            (scene.entity/get-id current))))))))

(defrecord Scene [window]
  SceneI
    (start [this] (-start this))
    (stop [this] (-stop this))
    (add-entity [this entity] (-add-entity this entity))
    (get-entities-by-model-id [this model-id]
      (-get-entities-by-model-id this model-id))
    (get-entities-map [this] (-get-entities-map this))
    (get-projection [this] (-get-projection this))
    (remove-all-entities [this] (-remove-all-entities this))
    (remove-entity [this entity] (-remove-entity this entity)))
