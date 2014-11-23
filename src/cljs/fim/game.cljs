(ns fim.game
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan put! <! timeout]]
            [goog.events :as events]
            [goog.dom :as gdom]
            [clojure.set :refer [union]]
            [clojure.string :as string]))

;; --------------------------------------------------------------------------------
;; Game info

(def game-speed "Game logic tick speed" 20)

(def loff { :on false :darkness 1.0 })
(def lon { :on true :darkness 0.0 })

;; Height and width of the canvas in pixels
(def width 640)
(def height 640)

(def light-speed 0.05)

(def initial-grids {
  3 [[lon  loff  loff
      loff loff  loff
      loff loff  loff]]
  5 [[lon  loff  loff  loff  loff
      loff loff  loff  loff  loff
      loff loff  loff  loff  loff
      loff loff  loff  loff  loff
      loff loff  loff  loff  loff]]
  7 [[lon  loff  loff  loff  loff  loff  loff
      loff loff  loff  loff  loff  loff  loff
      loff loff  loff  loff  loff  loff  loff
      loff loff  loff  loff  loff  loff  loff
      loff loff  loff  loff  loff  loff  loff
      loff loff  loff  loff  loff  loff  loff
      loff loff  loff  loff  loff  loff  loff]]})

(def initial-world {
  :status nil
  :speed game-speed
  :size 7
  :lights (get (get initial-grids 7) 0)
  })

;; --------------------------------------------------------------------------------
;; World Factory

(defn set-light-grid
  ([w a] (set-light-grid w a 0))
  ([w a b] (assoc (assoc w :size a) :lights (get (get initial-grids a) b))))

;; --------------------------------------------------------------------------------
;; Helpers

(defn clamp [l h x]
  (if (< x l) l
    (if (> x h) h x)))

;; --------------------------------------------------------------------------------
;; Lights out

(defn get-cell-width
  [size]
  (/ width size))

(defn get-cell-height
  [size]
  (/ height size))

(defn get-cell-x
  [counter size]
  (let [width (get-cell-width size)]
    (* (. js/Math (floor (/ counter size))) width)))

(defn get-cell-y
  [counter size]
  (let [height (get-cell-height size)]
    (* (mod counter size) height)))

(defn get-cell-counter
  [size]
  (dec (* size size)))

(def player-click (atom {
  :x -1
  :y -1
  }))

(defn is-click-on-light?
  [counter click-array]
  (some #(= counter %) click-array))

(defn get-index-from-xy [x y s] (+ y (* x s)))

(defn get-click-array
  [size click]
  (if (= (get click :x) -1) []
    (let [cx (- width (get click :x))
          cy (- height (get click :y))
          w (get-cell-width size)
          h (get-cell-height size)]
      (let [x (. js/Math (floor (/ cx w)))
            y (. js/Math (floor (/ cy h)))]
        (map
          (fn [{:keys [x y] :as p}] (get-index-from-xy x y size))
          (filter
            (fn [{:keys [x y] :as p}] (and (>= x 0) (< x size) (>= y 0) (< y size)))
            [{:x (+ x 0) :y (+ y 0)}
             {:x (+ x 1) :y (+ y 0)}
             {:x (- x 1) :y (+ y 0)}
             {:x (+ x 0) :y (+ y 1)}
             {:x (+ x 0) :y (- y 1)}]))))))

(defn update-light [{:keys [on darkness] :as light} counter size click-array]
  (let [is-on? (if (is-click-on-light? counter click-array) (not on) on)]{
  :on is-on?
  :darkness (clamp 0 1(if (and is-on? (> darkness 0.0))
            (- darkness light-speed)
            (if (and (not is-on?) (< darkness 1.0))
              (+ darkness light-speed)
              darkness)))}))

(defn update-lights [lights counter size click-array]
  (let [aux (fn aux [out in counter]
              (if (empty? in) out
                (aux (conj out (update-light (peek in) counter size click-array)) (pop in) (dec counter))))]
    (aux [] (into [] (rseq lights)) counter)))

;; --------------------------------------------------------------------------------
;; Mouse Logic

(defn listen [el type]
  (let [out (chan)]
    (events/listen el type
      (fn [e] (put! out e)))
    out))

(defn on-mouse-click
  [e]
  (let [x (.-offsetX e)
        y (.-offsetY e)]
    (reset! player-click {:x x :y y})))

(defn define-click-event []
  (let [clicks (listen (gdom/getElement "fim") "click")]
    (go (while true (on-mouse-click (<! clicks))))))

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
  (let [click (deref player-click)]
    (do
      (reset! player-click {:x -1 :y -1})
      { :status status
        :speed speed
        :size size
        :lights (update-lights lights (get-cell-counter size) size (get-click-array size click))
        })))

(defn game!
  "Game internal loop that processes commands and updates state applying functions"
  [initial-world cmds notify]
  (go-loop [{:keys [status speed size lights] :as world} initial-world]
    (let [[cmd v] (<! cmds)]
      (if (and (= status :game-over) (not= cmd :reset))
        (recur world)
        (case cmd
          :init (do (plan-tick! 0 cmds) (recur world))

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

          ;; :turbo (recur (update-speed world v))

          (throw (js/Error. (str "Unrecognized game command: " cmd))))))))

(defn init [commands]
  (let [notifos (chan)]
    (game! (set-light-grid initial-world 3) commands notifos)
    (define-click-event)
    notifos))

