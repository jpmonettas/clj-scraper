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
  [{name :name finder :finder follow :follow formatter :formatter} html url]
  (let [h (if follow
            (u/fetch-url (follow url html))
            html)
        effective-finder (or finder
                             #(u/html-sampler %))
        effective-formatter (or formatter identity)]
    {name (-> h
              (effective-finder)
              (effective-formatter))}))

(defmethod extract-attr :record
  [{name :name struct :entity follow :follow} html url]
  (let [u (follow url html)
        h (u/fetch-url u)]
    {name (extract struct h u)}))

(defn create-page-stream [url html next-page]
  (cons html (let [next-page-url (when next-page (next-page url html))
                   next-page-html (when (not (empty? next-page-url))
                                    (u/fetch-url next-page-url))]
               (when next-page-html
                 (lazy-seq (create-page-stream next-page-url next-page-html next-page))))))


(defn fetch-col-items [splitter limit next-page html url]
  (let [safe-take (fn [n col] (if n (take n col) col))]
    (->> (create-page-stream url html next-page)
         (map splitter)
         (reduce concat)
         (safe-take limit))))



(defmethod extract-attr :collection
  [{name :name item-structure :entity limit :limit splitter :splitter next-page :next-page} html url]
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; DSL implementation                                            ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-attr-type [attr]
  (cond (:splitter attr) :collection
        (:entity attr) :record
        :else :simple))

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
