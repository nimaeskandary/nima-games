(ns nimaeskandary.vulkan-tutorial.chapter-4.utils)

;; https://stackoverflow.com/questions/19845555/type-hint-a-nil-literal-in-clojure
;; There are lwjgl functions that have typed params but support null, and for
;; overloaded functions need to supply metadata for the types. Unfortunately
;; cannot do something like ^String nil directly at the call site
(def nil* nil)
