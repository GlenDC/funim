(ns fim.game
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan put! <! timeout]]))

;; --------------------------------------------------------------------------------
;; Game info

(def game-speed "Game logic tick speed" 300)

(def initial-world {
  :speed game-speed})

;; --------------------------------------------------------------------------------
;; Light Puzzle logic

;; todo

;; --------------------------------------------------------------------------------
;; Game logic

(defn plan-tick!
  "Tick the game after the elapsed speed time"
  ([speed cmds] (plan-tick! speed cmds (chan)))
  ([speed cmds shortcircuit]
   (go
    (alts! [(timeout speed) shortcircuit])
    (put! cmds [:tick]))))

(defn update-world
  "Applies the game constraints to the world and returns the new version."
  [{:keys [speed] :as world}]
  world)

(defn game!
  "Game internal loop that processes commands and updates state applying functions"
  [initial-world cmds notify]
  (go-loop [{:keys [status snake pills speed] :as world} initial-world]
    (let [[cmd v] (<! cmds)]
;;       (println "Received: " cmd)
      (if (and (= status :game-over) (not= cmd :reset))
        (recur world)
        (case cmd
          :init (do (plan-tick! 0 cmds) (recur world))

          :reset (do
                   (println "reset")
                   (if (= status :game-over) (put! cmds [:init]))
                   (recur initial-world))

          :tick (let [new-world (update-world world)
                      status (:status new-world)]
                  (if (= status :game-over)
                    (do
                      (>! notify [:game-over])
                      (recur new-world))
                    (do
                      (plan-tick! speed cmds)
                      (>! notify [status])
                      (>! notify [:world new-world])
                      (recur new-world))))

          :turn (println "reset")

          ;; :turbo (recur (update-speed world v))

          (throw (js/Error. (str "Unrecognized game command: " cmd))))))))

(defn init [commands]
  (let [notifos (chan)]
    (game! initial-world commands notifos)
    notifos))

