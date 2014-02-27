(ns clj-scraper.examples
  (:require [clj-scraper.core :refer :all]
            [clj-scraper.utils :refer :all])
  (:gen-class))

;; Ideas

;; (defentity twa-person
;;   [nombre :finder nil]
;;   [otra])

;; (defentity edad
;;   [])

;; (defentity twa-person
;;   [nombre :finder "/tr/td[4]/text()"]
;;   [otra :follow fn-rest-link :finder fn]
;;   [otroo :finder (xpath-selector "/tr/td[@class=\"statcol_num\"]/text()")])

;; (defentity twa-page
;;   [persons :splitter twa-person])


;; (def age [{:name :age-in-number
;;            :type :simple
;;            :finder ""}])

;; (def twa-person [{:name :position
;;               :type :simple
;;               :finder (xpath-selector "/tr/td[@class=\"statcol_num\"]/text()")}
;;              {:name :name
;;               :type :simple
;;               :finder (xpath-selector "/tr/td[@class=\"statcol_name\"]/text()")}
;;              {:name :age
;;               :type :simple
;;               :finder ""
;;               :follow  ""}])

;; (def twa-page [{:name :all
;;                 :type :collection
;;                 :col-type twa-person
;;                 :limit 10
;;                 :splitter (xpath-splitter "//*[@id=\"therest\"]/table/tbody/tr[position()>1]")
;;                 :next-page nil}])

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

(clojure.pprint/pprint (extract-url people "http://twitaholic.com/"))

;;(def testp (fetch-url "http://twitaholic.com/"))
;;(extract twa-page testp)
