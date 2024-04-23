(ns nimaeskandary.vulkan-tutorial.chapter-6.main
  (:require [nimaeskandary.vulkan-tutorial.chapter-6.eng.engine :as eng.engine]
            [nimaeskandary.vulkan-tutorial.chapter-6.app-logic :as app-logic]))

(defn -main
  []
  (let [app-logic (app-logic/start (app-logic/->AppLogic))
        engine (eng.engine/start (eng.engine/->Engine "Nima learns Vulkan"
                                                      app-logic))]
    (eng.engine/init engine)))
