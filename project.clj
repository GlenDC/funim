(defproject mgj-im "0.1.0-SNAPSHOT"
  :description "A game made for the Monser Game Jam in Brussels using the theme 'inner mechanic'."
  :url "https://github.com/glendc/mgj-im"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2371"]]

  :plugins [[lein-cljsbuild "1.0.4-SNAPSHOT"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds [{:id "mgj-im"
              :source-paths ["src/cljs"]
              :compiler {
                :output-to "bin/js/mgj_im.js"
                :output-dir "out"
                :optimizations :none
                :source-map true}}]})
