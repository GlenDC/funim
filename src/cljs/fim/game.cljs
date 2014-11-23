(ns fim.game
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [cljs.core.async :refer [chan put! <! timeout]]
            [goog.events :as events]
            [goog.dom :as gdom]
            [fim.utils.canvas :as canvas]
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
(def grow-speed 0.05)
(def fade-speed 0.04)

(def initial-grids {
  1 [[loff]]
  3 [[loff  lon  lon
      loff loff  loff
      lon lon  loff]]
  5 [[lon  loff  loff  loff  lon
      loff lon  loff  loff  lon
      loff loff  loff  loff  loff
      lon loff  lon  lon  loff
      loff loff  loff  lon  lon]]
  8 [[lon  loff  loff  loff  loff  loff  loff  loff
      loff loff  lon  loff  loff  lon  loff  loff
      loff loff  loff  loff  loff  loff  loff  loff
      loff loff  lon  lon  loff  loff  loff  lon
      loff lon  lon  lon  lon  loff  loff  loff
      lon loff  loff  loff  loff  loff  loff  loff
      loff loff  lon  loff  loff  loff  lon  loff
      loff loff  loff  loff  loff  loff  loff  loff]]
  16 [[lon  loff  loff  loff  loff  lon  lon  loff  loff  loff  loff  loff  loff  loff  loff  loff
      loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  lon  loff  loff  loff  loff  loff
      loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff
      loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff
      loff  loff  lon  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff
      loff  loff  loff  loff  loff  loff  lon  loff  loff  loff  loff  loff  loff  loff  lon  loff
      loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff
      loff  loff  loff  loff  loff  loff  loff  loff  lon  loff  loff  loff  loff  loff  loff  loff
      loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff
      loff  loff  loff  loff  lon  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff
      loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  lon
      loff  loff  lon  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff
      loff  lon  lon  loff  loff  loff  loff  loff  loff  loff  lon  loff  loff  loff  loff  loff
      loff  lon  lon  loff  lon  loff  loff  loff  loff  loff  loff  loff  loff  loff  lon  loff
      loff  lon  loff  loff  loff  loff  loff  loff  loff  lon  loff  loff  loff  loff  loff  loff
      loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  loff  lon  lon  lon]]})

(def initial-world {
  :status nil
  :speed game-speed
  :click { :x -1 :y -1 }
  :levels [16 8 5 3 1]
  :posi { :size 1 :tasi 1 }
  :lights (get (get initial-grids 7) 0)
  :menu-alpha 0.0
  })

(def image-loaded (atom false))

(def start-img (canvas/create-img "/data/start-screen.png"))

;; --------------------------------------------------------------------------------
;; World Factory

(defn set-light-grid
  ([w t] (set-light-grid w t 0))
  ([w t x]
    (let [wo (assoc w :lights (get (get initial-grids t) x))]
      (assoc wo :posi { :size 1 :tasi t }))))

(defn get-next-light-grid
  [wo]
  (let [levels (get wo :levels)]
    (let [level (peek levels)]
      (set-light-grid
        (assoc wo :levels (pop levels))
        level))))

;; --------------------------------------------------------------------------------
;; Helpers

(defn clamp [l h x]
  (if (< x l) l
    (if (> x h) h x)))

(defn floor [x] (. js/Math (floor x)))

(defn abs [x] (. js/Math (abs x)))

;; --------------------------------------------------------------------------------
;; Lights out

(defn is-locked? [s] (not (= (floor s) s)))

(defn get-cell-width
  [{:keys [size tasi] :as posi}]
  (* (/ width tasi) (/ tasi size)))

(defn get-cell-height
  [{:keys [size tasi] :as posi}]
  (* (/ height tasi) (/ tasi size)))

(defn get-cell-x
  [counter posi]
  (let [width (get-cell-width posi)]
    (* (floor (/ counter (get posi :tasi))) width)))

(defn get-cell-y
  [counter posi]
  (let [height (get-cell-height posi)]
    (* (mod counter (get posi :tasi)) height)))

(defn get-cell-counter
  [{:keys [tasi] :as posi}]
  (dec (* tasi tasi)))

(def player-click (atom {
  :x -1
  :y -1
  }))

(defn are-all-lights-on?
  [lights]
  (if (empty? lights) true
    (let [on (get (peek lights) :on)]
      (if (not on) false
        (are-all-lights-on? (pop lights))))))

(defn is-click-on-light?
  [counter click-array]
  (some #(= counter %) click-array))

(defn get-index-from-xy [x y posi] (+ y (* x (get posi :size))))

(defn get-click-array
  [posi click]
  (let [ox (get click :x)
        oy (get click :y)
        size (get posi :size)
        tasi (get posi :tasi)]
    (if (or (is-locked? size) (= ox -1)) []
      (let [cx (- width ox)
            cy (- height oy)
            w (get-cell-width posi)
            h (get-cell-height posi)]
        (let [x (floor (/ cx w))
              y (floor (/ cy h))]
          (map
            (fn [{:keys [x y] :as p}] (get-index-from-xy x y posi))
            (filter
              (fn [{:keys [x y] :as p}] (and (>= x 0) (< x size) (>= y 0) (< y size)))
              [{:x (+ x 0) :y (+ y 0)}
               {:x (+ x 1) :y (+ y 0)}
               {:x (- x 1) :y (+ y 0)}
               {:x (+ x 0) :y (+ y 1)}
               {:x (+ x 0) :y (- y 1)}])))))))

(defn update-light [{:keys [on darkness] :as light} counter posi click-array]
  (let [is-on? (if (is-click-on-light? counter click-array) (not on) on)]{
  :on is-on?
  :darkness (clamp 0 1(if (and is-on? (> darkness 0.0))
            (- darkness light-speed)
            (if (and (not is-on?) (< darkness 1.0))
              (+ darkness light-speed)
              darkness)))}))

(defn update-lights [lights counter posi click-array]
  (if (or (nil? lights) (empty? lights)) lights
    (let [aux (fn aux [out in counter]
                (if (empty? in) out
                  (aux (conj out (update-light (peek in) counter posi click-array)) (pop in) (dec counter))))]
      (aux [] (into [] (rseq lights)) counter))))

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
  [{:keys [status speed posi levels lights menu-alpha] :as world}]
  (let [click (deref player-click)
        tasi (get posi :tasi)
        size (get posi :size)]
    (let [lights (update-lights lights (get-cell-counter posi) posi (get-click-array posi click))]
      (do
        (reset! player-click {:x -1 :y -1})
        (let [new-world
          { :status status
            :speed speed
            :lights lights
            :levels levels
            :menu-alpha (clamp 0 1(if (> size 1)
                          (if (<= (abs menu-alpha) fade-speed) 0.0
                            (- menu-alpha fade-speed))
                          (if (<= (abs menu-alpha) fade-speed) 1.0
                            (+ menu-alpha fade-speed))))
            :posi {
              :tasi tasi
              :size (if (= size tasi) size
                        (if (< (abs (- tasi size)) grow-speed) tasi
                          (if (< tasi size)
                            (- size grow-speed)
                            (+ size grow-speed))))}}]
          (if (are-all-lights-on? lights) (get-next-light-grid new-world) new-world))))))

(defn game!
  "Game internal loop that processes commands and updates state applying functions"
  [initial-world cmds notify]
  (go-loop [{:keys [status speed posi lights] :as world} initial-world]
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
          :reset (do
            (if (put! cmds [:init]))
              (recur initial-world))

          ;; :turbo (recur (update-speed world v))

          (throw (js/Error. (str "Unrecognized game command: " cmd))))))))

(defn init [commands]
  (let [notifos (chan)]
    (game! (get-next-light-grid initial-world) commands notifos)
    (define-click-event)
    notifos))

