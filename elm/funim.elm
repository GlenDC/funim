module Funim where

import Color exposing (..)
import Graphics.Collage exposing (..)
import Graphics.Element exposing (..)
import Window
import Mouse
import Array exposing (Array)
import Text
import Debug

-- TODO:
---- Fix Positioning grid
------ Need Title + one line instruction [TOP]
------ Ned Control instructions [BOTTOM]
---- Replace lines by squares
------ this should be based on the state (on / off)
---- Add Mouse Click Signal (left)
------ check if on Square, if so change state
---- Game logic -> If complete light or dark -> Next Level
---- store game logic in browser coockie
---- add reset button
---- Add 5 Levels
---- Make Levels generate automatically based on
------ Steps Sequence (logic)
------ Rules (only generated solvable levels)

levelsDatabase = Array.fromList [
  [ False ], -- level 1, Nv. 1
  [ False, True, True, -- level 2, Nv. 3
    False, False, False,
    True, True, False ],
  [ True, False, False, False, True, -- level 3, Nv. 5
    False, True, False, False, True,
    False, False, False, False, False,
    True, False, True, True, False,
    False, False, False, True, True ] ]

getLevel : Int -> List Bool
getLevel level =
  case (Array.get level levelsDatabase) of
    Just list -> list 
    Nothing -> []

type alias State = { level : Int, grid : List Bool }

getInitialGameState : Bool -> State
getInitialGameState checkCache =
  { level = 0, grid = getLevel 1 }

input = Signal.map2 (,) Mouse.isDown Mouse.position

main : Signal Element
main =
  Signal.map2 render Window.dimensions
    (Signal.foldp update (getInitialGameState True) input)

calculateOffset : Float -> Int -> Float
calculateOffset cellsz icell =
  let evenify = toFloat (icell - (icell % 2))
  in ((evenify / 2.0) * cellsz) |> negate

renderCell : Bool -> Float -> Float -> Float -> Form
renderCell isLightOn x y size =
  let clr = if isLightOn then lightGrey else darkGrey
  in move (x, y) (filled clr (square size))

renderGrid : List Bool -> Int -> Float -> Float -> List Form -> List Form
renderGrid state length size offset forms =
  case state of
    [] -> forms
    hd :: rest ->
      let l = (List.length state) - 1
          xi = toFloat (l % length) 
          yi = toFloat (floor ((toFloat l) / (toFloat length)))
          x = offset + (xi * size)
          y = offset + (yi * size)
      in renderGrid rest length size offset (
        (renderCell hd x y size) :: forms)

renderText : String -> Float -> Float -> Float -> Form
renderText content x y sz =
  move (x, y) (Text.fromString content
    |> Text.typeface [ "Helvetica" ]
    |> Text.color darkGrey |> Text.height sz |> text)

render : (Int, Int) -> State -> Element
render (x, y) state =
  let smallest = toFloat (min x y)
      padding = smallest / 8
      contextsz = round (smallest)
      gridLength = state.grid |> List.length |> toFloat |> sqrt |> floor
      gridsz = (toFloat contextsz) - (padding * 2)
      cellsz = gridLength |> toFloat |> (/) gridsz
      offset = calculateOffset cellsz gridLength
      h1sz = padding / 2.5
      h2sz = padding / 3.5
      halfsz = smallest / 2.0
  in
    collage contextsz contextsz
      ((renderText "Light || Dark"
          0 (halfsz - h1sz) h1sz) ::
       (renderText "Toggle any light by clicking on it."
          0 (negate (halfsz - (h2sz * 2))) h2sz) ::
       (renderGrid state.grid gridLength cellsz offset []))

toggleGrid : List Bool -> Int -> Int -> List Bool
toggleGrid lights mx my =
  lights

update : (Bool, (Int, Int)) -> State -> State
update (hasLeftClicked, (mx, my)) state =
  if hasLeftClicked then
    let newGrid = toggleGrid state.grid mx my
    in { state | grid <- newGrid }
  else state
