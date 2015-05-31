module Funim where

import Color exposing (..)
import Graphics.Collage exposing (..)
import Graphics.Element exposing (..)
import Window
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
gameState = { level = 0, grid = getLevel 0}

main : Signal Element
main =
  Signal.map render Window.dimensions

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
        (renderCell hd x y size) ::
          (((toString (floor x)) ++ ", " ++ (toString (floor y))) |> Text.fromString |> text |> move (x, y)):: forms)

renderText : String -> Float -> Float -> Float -> Form
renderText content x y sz =
  move (x, (y - sz)) (Text.fromString content
    |> Text.typeface [ "Helvetica" ]
    |> Text.color darkGrey |> Text.height sz |> text)

render : (Int, Int) -> Element
render (x, y) =
  let smallest = toFloat (min (min x y) 1080)
      padding = smallest / 8
      contextsz = round (smallest)
      gridLength = gameState.grid |> List.length |> toFloat |> sqrt |> round
      gridsz = (toFloat contextsz) - (padding * 2)
      cellsz = gridLength |> toFloat |> (/) gridsz
      hcellsz = cellsz / 2
      nhgridsz = gridsz |> (/) 2.0 |> (+) padding |> (+) hcellsz |> negate
  in
    collage contextsz contextsz
      ((renderText "Light || Dark" 0 (smallest / 2.0) (padding / 2.5)) ::
       (renderText "Toggle any light by clicking on it." 0 (negate (smallest / 4.0)) (padding / 3.5)) ::
       (renderGrid gameState.grid gridLength cellsz nhgridsz []))
