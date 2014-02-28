(ns clj-scraper.examples
  (:require [clj-scraper.core :refer :all]
            [clj-scraper.utils :refer :all])
  (:gen-class))



(def people-spl (memoize (xpath-splitter "//*[@id=\"therest\"]/table/tbody/tr[position()>1]")))

(defentity person
  [position :finder (xpath-selector "/tr/td[1]/text()")]
  [name :finder (xpath-selector "/tr/td[3]/a/span/text()")]
  [followers :finder (xpath-selector "/tr/td[6]/a/text()")]
  [moto :follow (fn [u,h] (str u ((xpath-selector "/tr/td[3]/a/@href") h))) :finder (xpath-selector "//*[@id=\"bio\"]/text()")])

(defentity people
  [people :splitter people-spl
   :limit 2
   :col-type person])

(comment


  (clojure.pprint/pprint (extract-url people "http://twitaholic.com/"))


 )
