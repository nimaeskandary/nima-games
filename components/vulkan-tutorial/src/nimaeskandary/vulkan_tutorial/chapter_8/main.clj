(ns nimaeskandary.vulkan-tutorial.chapter-8.main
  (:require [nimaeskandary.vulkan-tutorial.chapter-8.eng.engine :as eng.engine]
            [nimaeskandary.vulkan-tutorial.chapter-8.app-logic :as app-logic]))

(defn -main
  []
  (let [app-logic (app-logic/start (app-logic/->AppLogic))
        engine (eng.engine/start (eng.engine/->Engine "Nima learns Vulkan"
                                                      app-logic))]
    (eng.engine/init engine)))
