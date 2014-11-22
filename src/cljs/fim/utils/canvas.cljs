(ns fim.utils.canvas)

(defn context [canvas] (.getContext canvas "2d"))

(defn set-dimensions! [canvas w h]
  (set! (.-width canvas) w)
  (set! (.-height canvas) h))

(defn hsla
  "Returns a string with the hsla color.
  h(ue) is a value between 0 and 360
  s(aturation) is a value between 0 and 100
  l(ightness) is a value between 0 and 100
  a(lpha) [optional] between 0 and 1"
  ([h s l] (hsla h s l 1))
  ([h s l a]
   (str "hsla(" h "," s "%," l "%," a ")")))

(defn rgba
  "Returns a string with the rgba color.
  r(red) is a value between 0 and 255
  g(green) is a value between 0 and 255
  b(blue) is a value between 0 and 255
  a(lpha) [optional] between 0 and 1"
  ([r g b] (rgba r g b 1))
  ([r g b a]
   (str "rgba(" r "," g "," b "," a ")")))

(defn save! [ctx] (.save ctx))

(defn restore! [ctx] (.restore ctx))

(defn translate! [ctx x y] (.translate ctx x y))

(defn fill-style! [ctx color]
  (set! (.-fillStyle ctx) (apply rgba color)))

(defn fill-rect! [ctx x y w h] (.fillRect ctx x y w h))
(defn stroke-rect! [ctx x y w h] (.strokeRect ctx x y w h))

(defn clear-rect! [ctx x y w h]
  (do
    ;;(.clearRect ctx x y w h)
    (fill-style! ctx [50, 50, 50])
    (fill-rect! ctx x y w h)))

(def deg360 (* 2 (.-PI js/Math)))
(defn fill-circle! [ctx x y r]
  (doto ctx
    (.beginPath)
    (.arc x y r 0 deg360 true)
    (.fill)))

(defn stroke-circle! [ctx x y r]
  (doto ctx
    (.beginPath)
    (.arc x y r 0 deg360 true)
    (.stroke)))

(defn font
  "Returns a string with the font parsed."
  [font style size] (str style " " size "pt " font))

(defn font! [ctx family style size]
  (set! (.-font ctx) (font family style size)))

(defn text-align! [ctx align]
  (set! (.-textAlign ctx) align))

(defn fill-text! [ctx text x y] (.fillText ctx text x y))

(defn request-animation-frame [f] (js/requestAnimationFrame f))
