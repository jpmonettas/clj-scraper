(ns clj-scraper.core
  (:require [net.cgrand.enlive-html :as e]
            [clj-xpath.core :as xp]
            [hiccup.util :as hutil]
            [clojure.xml :as xml]
            [clj-scraper.utils :as u])
  (:use clojure.tools.trace)
  (:gen-class))


(declare extract)

(defmulti extract-attr (fn [structure html url] (:type structure)))

(defmethod extract-attr :simple
  [{name :name finder :finder follow :follow} html url]
  (let [h (if follow
            (u/fetch-url (follow url html))
            html)
        effective-finder (or finder
                             #(u/html-sampler %))]
    {name (effective-finder h)}))

(defn fetch-col-items [splitter limit next-page html url]
  ((if limit (partial take limit) identity)
        (loop [res (splitter html)
               h html]
          (if (or (empty? res)
                  (nil? next-page)
                  (and limit (> (count res) limit)))
            res
            (let [next-page-url (next-page url h)
                  next-page-content (u/fetch-url next-page-url)
                  next-page-items (splitter next-page-content)]
              (if (or (empty? next-page-url)
                      (empty? next-page-items))
                res
                (recur (concat res next-page-items) next-page-content)))))))



(defmethod extract-attr :collection
  [{name :name item-structure :col-type limit :limit splitter :splitter next-page :next-page} html url]
  (let [splitted-col (->>
                      (fetch-col-items splitter limit next-page html url)
                      (map #(extract item-structure % url)))]
    {name splitted-col}))

(defn extract
  "Returns a map with extracted data from html with the given structure.
structure is a vector of attributes"
  [structure html url]
  (if (vector? structure)
    (->> structure
         (map #(extract-attr % html url))
         (reduce merge {}))
    (u/html-sampler html)))

(defn extract-url [structure url]
  (extract structure (u/fetch-url url) url))


(defn get-attr-type [attr]
  (if (:splitter attr)
    :collection
    :simple))

(defn process-attribute [attr]
  (let [[attr-name & attr-rest] attr
        attribute (merge {:name (keyword attr-name)}
                         (apply hash-map attr-rest))]
    (assoc attribute :type (get-attr-type attribute))))

(defn entity-attribute [attributes]
  (->> attributes
      (map process-attribute)))

(defmacro defentity [name & attributes]
  `(def ~name [~@(entity-attribute attributes)]))
