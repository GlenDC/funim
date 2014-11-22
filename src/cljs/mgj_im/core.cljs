(ns mgj-im.core
  (:require [mgj-im.res :as res]
            [mgj-im.game :as game]
    ))

(enable-console-print!)

;; mutable global data

(def global-data {
  :iterator 0
  })

;; main game

(def main (js-obj
  "resources" res/resources
  "onload" (fn []
    (this-as t
      (if (not (. js/me.video (init "screen" js/me.video.CANVAS 640 480 true "auto")))
          (js/alert "Your browser does not support HTML5 canvas.")
          (do (if (= js/document.location.hash "#debug")
                  (.  js/me.plugin.register (defer t js/me.debug.Panel "debug" js/me.input.KEY.V)))
              (. js/me.audio (init "mp3,ogg"))
              (set! js/me.loader.onload (. t.loaded (bind t)))
              (. js/me.loader (preload main.resources))
              (. js/me.state (change js/me.state.LOADING))))))
  "loaded" (fn []
    (do
      (. js/me.state (set js/me.state.GAME_SCREEN (game/game-screen.)))
      (. js/me.state (change js/me.state.GAME_SCREEN))
      (println "Loaded!")))))

