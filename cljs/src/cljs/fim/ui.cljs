(ns fim.ui
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [fim.utils.dom :as dom]
            [fim.utils.canvas :as canvas]
            [fim.game :as game]
            [cljs.core.async :as async :refer [chan put! pipe unique merge map< filter< alts!]]
            [goog.events :as events]
            [goog.dom :as gdom]
            [clojure.set :refer [union]]
            [clojure.string :as string]))

(def world "UI state" (atom nil))

;; -------------------------------------------------------------------------------
;; Canvas UI

(def cvs
  "Canvas dom element on the page"
  (dom/by-id "fim"))

(def ctx
  "Canvas 2d context for drawing"
  (canvas/context cvs))

(def black [0 0 0])
(def white [255 255 255])

(def clon [1 0])

(defn resize-canvas!
  "Resize canvas to the required size for the board"
  [] (canvas/set-dimensions! cvs game/width game/height))

(defn draw-timestamp!
  "Draw a timestamp in the bottom right of the canvas"
  [ctx]
  (canvas/font! ctx "monospace" 12 "normal")
  (canvas/text-align! ctx "right")
  (canvas/fill-style! ctx [255 0 0])
  (canvas/fill-text! ctx (.toLocaleString (js/Date.)) (- game/width 10) (- game/height 10)))

(defn draw-text!
  "Draw a text in the screen of the canvas"
  [ctx text]
  (canvas/font! ctx "monospace" 16 "bold")
  (canvas/text-align! ctx "center")
  (canvas/fill-style! ctx [0 0 0])
  (let [lines (.split text "\n")
        n (quot (count lines) 2)]
    (doseq [[line y] (map vector lines (range (- n) (+ n 1)))]
      (canvas/fill-text! ctx line (quot game/width 2) (+ (quot game/height 2) (* 18 y))))))

(defn draw-game-over! [ctx]
  (draw-text! ctx "GAME OVER\nPress Enter to restart"))

;; -------------------------------------------------------------------------------
;; UI data fns

(defn game-world->ui-world
  "Transforms a game snapshot into ui world data"
  [{:keys [posi lights menu-alpha] :as world}]
  {
    :lights lights
    :posi posi
    :menu-alpha menu-alpha})

;; -------------------------------------------------------------------------------
;; Render logic

(defn draw-light-grid [ctx lights counter posi width height]
  (if (and (not (empty? lights)) (>= counter 0))
    (do
      (canvas/fill-style! ctx (conj black (get (peek lights) :darkness)))
      (let [x (game/get-cell-x counter posi)
            y (game/get-cell-y counter posi)]
        (do ;;(println (str x " " y " " width " " height ))
            (canvas/fill-rect! ctx (dec x) (dec y) (+ width 2) (+ height 2))
            (draw-light-grid ctx (pop lights) (dec counter) posi width height))))))

(defn render
  "Render"
  [{:keys [posi lights menu-alpha] :as world}]
  (if world
    (do
      (canvas/clear-rect! ctx 0 0 game/width game/height)
      (canvas/save! ctx)
      (canvas/translate! ctx 0.5 0.5) ; To avoid blurry lines
      (draw-light-grid ctx lights (game/get-cell-counter posi) posi (game/get-cell-width posi) (game/get-cell-height posi))
      ;;(draw-timestamp! ctx)
      (if (> menu-alpha 0.0)
        (canvas/draw-image ctx game/start-img 0 0 game/width game/height menu-alpha))
      (canvas/restore! ctx)))
  )

(defn render-loop!
  "Render loop. Watches for updates from the game and sets the
  actual loop that renders"
  [notifos]
  (go-loop [[cmd v] (<! notifos)]
;;       (println cmd v)
      (case cmd
        :world (reset! world (game-world->ui-world v))
        :game-over (do (reset! world nil) (draw-game-over! ctx))
        (println (js/Error. (str "Unrecognized UI command: " cmd))))
    (recur (<! notifos)))

  (canvas/request-animation-frame (fn lo []
                                    (canvas/request-animation-frame lo)
                                    (render @world))))

;; -------------------------------------------------------------------------------
;; UI starting point

(defn init
  "Initialize the UI by initializing the user input, adapting the canvas
  and starting the render loop."
  []
  (let [commands (chan)
        notifos (game/init commands)]
    (resize-canvas!)
    (render-loop! notifos)
    (put! commands [:init])
    ))
