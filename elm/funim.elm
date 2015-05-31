module Funim where

import Color exposing (..)
import Graphics.Collage exposing (..)
import Graphics.Element exposing (..)
import Graphics.Input exposing (..)
import Window
import Mouse
import Array exposing (Array)
import Text
import Maybe

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

getLevel : Int -> (Int, List Bool)
getLevel level =
  case (Array.get level levelsDatabase) of
    Just list -> (level, list)
    Nothing ->
      if | level < 0 -> getLevel ((Array.length levelsDatabase) - 1)
         | otherwise -> getLevel 0

type alias State = { level : Int, count : Int, grid : List Bool }

getNewLevel : Int -> State
getNewLevel level' =
  let (level, grid) = getLevel level'
      count = grid |> List.length |> toFloat |> sqrt |> floor
  in { level = level, count = count, grid = grid }


getInitialGameState : Bool -> State
getInitialGameState checkCache =
  getNewLevel 0

lightQuery : Signal.Mailbox Int
lightQuery = Signal.mailbox 0

main : Signal Element
main =
  Signal.map2 render Window.dimensions
    (Signal.foldp update (getInitialGameState True) lightQuery.signal)

calculateOffset : Float -> Int -> Float
calculateOffset cellsz icell =
  let evenify = toFloat (icell - (icell % 2))
  in ((evenify / 2.0) * cellsz) |> negate

createLight : Bool -> Float -> Float -> Float -> Int -> Form
createLight isOn x y size i =
  let colour = if isOn then yellow else (greyscale 0.6)
      ctsz = floor (size)
      light = collage ctsz ctsz [(filled colour (circle (size / 2.5)))]
  in move (x, y) (toForm (customButton
    (Signal.message lightQuery.address i)
    light light light))

renderGrid : List Bool -> Int -> Float -> Float -> List Form -> List Form
renderGrid state length size offset forms =
  case state of
    [] -> forms
    hd :: rest ->
      let totalLength = length * length
          l = totalLength - (List.length state)
          xi = toFloat (l % length) 
          yi = toFloat (floor ((toFloat l) / (toFloat length)))
          x = offset + (xi * size)
          y = offset + (yi * size)
      in renderGrid rest length size offset (
        (createLight hd x y size l) :: forms)

renderText : String -> Float -> Float -> Float -> Form
renderText content x y sz =
  move (x, y) (Text.fromString content
    |> Text.typeface [ "Helvetica" ]
    |> Text.color darkGrey |> Text.height sz |> text)

getLevelDesc : Int -> String
getLevelDesc level =
  case level of
    0 -> "Toggle any light by clicking on it."
    1 -> "Surrounding lights are effected to!"
    2 -> "Can you still handle it?"
    _ -> ""

render : (Int, Int) -> State -> Element
render (x, y) state =
  let smallest = toFloat (min x y)
      padding = smallest / 8
      contextsz = round (smallest)
      gridsz = (toFloat contextsz) - (padding * 2)
      cellsz = state.count |> toFloat |> (/) gridsz
      offset = calculateOffset cellsz state.count
      h1sz = padding / 2.5
      h2sz = padding / 3.5
      halfsz = smallest / 2.0
  in
    collage x y [
      (filled (greyscale 0.75) (rect (toFloat x) (toFloat y))),
      toForm (collage contextsz contextsz
        ((renderText "Light || Dark"
            0 (halfsz - h1sz) h1sz) ::
         (renderText (getLevelDesc state.level)
            0 (negate (halfsz - (h2sz * 2))) h2sz) ::
         (renderGrid state.grid state.count cellsz offset []))
       |> container x y middle)]

updateLight : Int -> Int -> Int -> Bool -> Bool
updateLight i target count isOn =
  let toggle = not isOn
      pos = i - target
  in if | pos == 0 -> toggle
        | pos == count -> toggle
        | pos == (negate count) -> toggle
        | otherwise ->
           let cy = floor ((toFloat i) / (toFloat count))
               ty = floor ((toFloat target) / (toFloat count))
           in if | pos == 1 && (cy == ty) -> toggle
                 | pos == (negate 1) && (cy == ty) -> toggle
                 | otherwise -> isOn

updateGrid : Int -> Int -> Int -> List Bool -> List Bool -> List Bool
updateGrid i target count lights aux =
  case lights of
    [] -> List.reverse aux
    hd::tl ->
      updateGrid (i + 1) target count tl
        ((updateLight i target count hd) :: aux)

areWeFinished : Bool -> List Bool -> Bool
areWeFinished isOn lights =
  case lights of
    [] -> True
    hd::tl -> if hd == isOn then (areWeFinished isOn tl) else False

update : Int -> State -> State
update i state =
  let newGrid = updateGrid 0 i state.count state.grid []
  in
    if (areWeFinished
          (Maybe.withDefault False (List.head newGrid))
          (Maybe.withDefault [] (List.tail newGrid)))
      then getNewLevel (state.level + 1)
      else { state | grid <- newGrid }
