(ns user
  (:require [nrepl.server :refer [start-server]]))

(defonce nrepl (start-server :port 7888))
