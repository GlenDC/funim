module Funim where

import Color exposing (..)
import Graphics.Collage exposing (..)
import Graphics.Element exposing (..)
import Window
  
main : Signal Element
main =
  Signal.map render Window.dimensions

renderGridLine : (Int, Int, Int, Int) -> Form
renderGridLine (ax, ay, bx, by) =
  traced (dashed grey) (path ([(toFloat ax, toFloat ay), (toFloat bx, toFloat by)]))

renderGridLines : Int -> Int -> ((Int,  Int) -> Form) -> List Form
renderGridLines (size, i, renderLine) =
  if i == 0 then [] else
    let i = i - 1
        pos = i * size 
    in (renderLine pos size) :: renderGridLines size i renderLine

renderGrid : (Int, Int, Int) -> Element
renderGrid (size, gx, gy) =
  let renderHor = \pos size -> (renderGridLine pos 0 pos size)
      renderVer = \pos size -> (renderGridLine 0 pos size pos)
      grid = renderGridLines size gy renderHor ++
        renderGridLines size gx renderVer
  in
    collage size size grid

render : (Int, Int) -> Element
render (x, y) =
  renderGrid (x, 4, 4)
