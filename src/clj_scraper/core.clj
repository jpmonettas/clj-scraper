(ns clj-scraper.core
  (:require [net.cgrand.enlive-html :as e]
            [clj-xpath.core :as xp]
            [hiccup.util :as hutil]
            [clojure.xml :as xml]
            [clj-scraper.utils :as u])
  (:use clojure.tools.trace)
  (:gen-class))



(declare extract)

(defmulti extract-attr (fn [structure html] (:type structure)))

(defmethod extract-attr :simple
  [{name :name finder :finder} html]
  (let [effective-finder (or finder
                             #(u/html-sampler %))]
    {name (effective-finder html)}))


(defmethod extract-attr :collection
  [{name :name item-structure :col-type limit :limit splitter :splitter} html]
  (let [splitted-col (->>
                      (splitter html)
                      (map #(extract item-structure %)))]
    {name splitted-col}))

(defn extract
  "Returns a map with extracted data from html with the given structure.
structure is a vector of attributes"
  [structure html]
  (if (vector? structure)
    (->> structure
         (map #(extract-attr % html))
         (reduce merge {}))
    (u/html-sampler html)))
