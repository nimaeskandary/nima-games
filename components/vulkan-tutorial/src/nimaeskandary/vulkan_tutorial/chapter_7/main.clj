(ns nimaeskandary.vulkan-tutorial.chapter-7.main
  (:require [nimaeskandary.vulkan-tutorial.chapter-7.eng.engine :as eng.engine]
            [nimaeskandary.vulkan-tutorial.chapter-7.app-logic :as app-logic]))

(defn -main
  []
  (let [app-logic (app-logic/start (app-logic/->AppLogic))
        engine (eng.engine/start (eng.engine/->Engine "Nima learns Vulkan"
                                                      app-logic))]
    (eng.engine/init engine)))
