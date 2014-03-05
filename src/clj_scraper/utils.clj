(ns clj-scraper.utils
  (:require [net.cgrand.enlive-html :as e]
            [clj-xpath.core :as xp]
            [hiccup.util :as hutil]
            [clojure.xml :as xml])
  (:use clojure.tools.trace)
  (:gen-class))

(def fetch-url (memoize slurp))

(defn cleaner [& chars]
  (fn [html-str]
    (apply str (remove (into #{} chars) html-str))))

(defn regexp-selector [regexp n]
  (fn [html-str]
    (try
      (when html-str
        (nth (re-matches regexp html-str) n))
      (catch Exception e (.printStackTrace e)))))

(defn enlive-selector [selector]
  (fn [html-str]
    (apply str(-> html-str
                   (e/html-snippet)
                   (e/select selector)
                   (e/emit*)))))

(defn enlive-splitter [selector]
  (fn [html-str]
    (->>
     (-> html-str
         (e/html-snippet)
         (e/select selector))
     (map #(apply str (e/emit* %))))))

(defn remove-xml-header [xml]
  (clojure.string/replace xml #"<\?xml.*\?>" ""))


(defn sanitize-html [html-str]
  (apply str
         (-> html-str
             (e/html-snippet)
             (e/emit*))))

(defn xpath-splitter [selector]
  (fn [html-str]
    (map #(remove-xml-header (xp/node->xml (:node %)))
         (xp/$x selector (sanitize-html html-str)))))

(defn xpath-selector [selector]
  (fn [html-str]
    (remove-xml-header
     (xp/node->xml
      (:node (first (xp/$x selector (sanitize-html html-str))))))))

(def html-sampler-size 20)

(defn html-sampler [html-str]
  (let [sample-end (min html-sampler-size (count html-str))]
    (str
     (subs html-str 0 sample-end )
     (when (< sample-end (count html-str)) "..."))))
