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
gameState = { level = 1, grid = getLevel 1 }

main : Signal Element
main =
  Signal.map render Window.dimensions

renderCell : Bool -> Float -> Float -> Float -> Form
renderCell isLightOn x y size =
  let clr = if (Debug.log "isLightOn: " isLightOn) then green else red
  in move ((Debug.log "X: " x), (Debug.log "Y: " y)) (filled clr (square size))

renderGrid : List Bool -> Int -> Float -> Float -> List Form -> List Form
renderGrid state length padding size forms =
  case state of
    [] -> forms
    hd :: rest ->
      let l = (List.length state) - 1
          x = padding + ((toFloat (l % length)) * size)
          y = padding + ((toFloat (round ((toFloat l) / (toFloat length)))) * size)
          npadding = negate padding
      in renderGrid rest length padding size ((renderCell hd x y size) :: forms)

render : (Int, Int) -> Element
render (x, y) =
  let smallest = toFloat (min x y)
      padding = smallest / 8
      contextsz = round (smallest)
      gridLength = round (sqrt (toFloat (List.length gameState.grid)))
  in
    collage contextsz contextsz
      ((move (0, (smallest / 2)) (Text.fromString "Light || Dark"
        |> Text.typeface [ "Helvetica" ]
        |> Text.color grey
        |> Text.bold
        |> Text.height (padding / 2)
        |> text)) ::
      (renderGrid gameState.grid gridLength padding
        ((toFloat contextsz) / (toFloat gridLength)) []))
