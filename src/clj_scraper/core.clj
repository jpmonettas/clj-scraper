(ns clj-scraper.core
  (:require [net.cgrand.enlive-html :as html]
            [clj-xpath.core :as xp])
  (:use clojure.tools.trace)
  (:gen-class))


(def fetch-url (memoize slurp))

(declare extract)

(defmulti extract-attr (fn [structure html] (:type structure)))

(defmethod extract-attr :simple
  [{name :name finder :finder} html]
  (let [effective-finder (or finder identity)]
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
    html))

;; Some helpers that should be moved to a different ns
(defn enlive-selector [selector]
  (fn [html-str]
    (apply str(-> html-str
                   (html/html-snippet)
                   (html/select selector)
                   (html/emit*)))))

(defn enlive-splitter [selector]
  (fn [html-str]
    (->>
     (-> html-str
         (html/html-snippet)
         (html/select selector))
     (map #(apply str (html/emit* %))))))

(defn remove-xml-header [xml]
  (clojure.string/replace xml #"<\?xml.*\?>" ""))

(defn xpath-splitter [selector]
  (fn [html-str]
    (map #(remove-xml-header (xp/node->xml (:node %)))
         (xp/$x selector html-str))))

(defn xpath-selector [selector]
  (fn [html-str]
    (remove-xml-header
     (xp/node->xml
      (:node (first (xp/$x selector html-str)))))))

;; Some examples
(def hn-new [{:name :position
              :type :simple
              :finder nil}
             {:name :description
              :type :simple
              :finder nil}
             {:name :reference
              :type :simple
              :finder nil}])

(def twa-page [{:name :all
               :type :collection
               :col-type nil
               :limit 10
               :splitter (xpath-splitter "//*[@id=\"therest\"]/table/tbody/tr[position()>1]")}])

;;(extract twa-page (fetch-url "http://twitaholic.com/"))
