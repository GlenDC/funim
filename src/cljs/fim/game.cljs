(ns fim.game
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan put! <! timeout]]))

;; --------------------------------------------------------------------------------
;; Game info

(def game-speed "Game logic tick speed" 20)

(def loff { :on false :energy 0.0 })
(def lon { :on true :energy 1.0 })

(def light-speed 0.05)

(def initial-grids {
  3 [lon  loff  loff
     loff loff  loff
     loff loff  loff]
  5 [lon  loff  loff  loff  loff
     loff loff  loff  loff  loff
     loff loff  loff  loff  loff
     loff loff  loff  loff  loff
     loff loff  loff  loff  loff]})

(def initial-world {
  :status nil
  :speed game-speed
  :size 3
  :lights (get initial-grids 3)
  })

;; --------------------------------------------------------------------------------
;; Lights out

(defn update-light [{:keys [on energy] :as light}] {
  :on on
  :energy (if (and on (> energy 0.0))
            (- energy light-speed)
            (if (and (not on) (< energy 1.0))
              (+ energy light-speed)
              energy))})

(defn update-lights [lights]
  (let [aux (fn aux [out in]
              (if (empty? in) out
                (aux (conj out (update-light (peek in))) (pop in))))]
    (aux [] (into [] (rseq lights)))))

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
  [{:keys [status speed size lights] :as world}]
  {
    :status status
    :speed speed
    :size size
    :lights (update-lights lights)
    })

(defn game!
  "Game internal loop that processes commands and updates state applying functions"
  [initial-world cmds notify]
  (go-loop [{:keys [status speed size lights] :as world} initial-world]
    (let [[cmd v] (<! cmds)]
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

          :turn (println (str "turn: " v))

          ;; :turbo (recur (update-speed world v))

          (throw (js/Error. (str "Unrecognized game command: " cmd))))))))

(defn init [commands]
  (let [notifos (chan)]
    (game! initial-world commands notifos)
    notifos))

