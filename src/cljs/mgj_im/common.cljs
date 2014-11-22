(ns mgj-im.common)

;; helper function to easily init a melonjs object
(defn init [object properties]
  (do
    (for [[property value] properties]
      (aset object property value))
    object))