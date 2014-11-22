(ns mgj-im.core
  (:require [mgj-im.res :as res]))

(enable-console-print!)

(def main (js-obj
  "resources" res/resources
  "onload" (fn []
    (println "It's Loaded!")
  )))
