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

(defn fetch-col-items [splitter limit next-page html]
  ((if limit (partial take limit) identity)
        (loop [res (splitter html)
               h html]
          (if (or (empty? res)
                  (nil? next-page)
                  (and limit (> (count res) limit)))
            res
            (let [next-page-url (next-page "" h)
                  next-page-content (u/fetch-url next-page-url)
                  next-page-items (splitter next-page-content)]
              (if (or (empty? next-page-url)
                      (empty? next-page-items))
                res
                (recur (concat res next-page-items) next-page-content)))))))

(defmethod extract-attr :collection
  [{name :name item-structure :col-type limit :limit splitter :splitter next-page :next-page} html]
  (let [splitted-col (->>
                      (fetch-col-items splitter limit next-page html)
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


(defn get-attr-type [attr]
  (if (:splitter attr)
    :collection
    :simple))

(defn process-attribute [attr]
  (let [[attr-name & attr-rest] attr]
    (merge {:name (keyword attr-name) :type (get-attr-type attr-rest)}
           (apply hash-map attr-rest))))

(defn entity-attribute [attributes]
  (->> attributes
      (map process-attribute)))

(defmacro defentity [name & attributes]
  `(def ~name [~@(entity-attribute attributes)]))
