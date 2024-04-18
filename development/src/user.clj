(ns user
  (:require [nrepl.server :refer [start-server]]))

(set! *warn-on-reflection* true)
(defonce nrepl (start-server :port 7888))
