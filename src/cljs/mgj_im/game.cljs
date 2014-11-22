(ns mgj-im.game
  (:require [mgj-im.common :as common]))

;; HUD and HUD Elements

(def game-hud (js-obj
  "Container" (common/init (js/me.Container.) {
    "isPersistent" true
    "floating" true
    "z" js/Infinity
    "name" "HUD"})))

;; Game Screen

(def game-screen js/me.ScreenObject)

(set! game-screen.onResetEvent (fn [_]
  (this-as t (do
    (set! t.HUD (game-hud.Container.))
    (. js/me.game.world (addChild t.HUD))))))