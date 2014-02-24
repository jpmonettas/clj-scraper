(ns clj-scraper.core-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [hiccup.core :as h]
            [clj-scraper.core :refer :all]
            [clj-scraper.utils :refer :all]
            [net.cgrand.enlive-html :as e]))


(facts "about extract-attr simple"
       (let [example (h/html [:tr
                               [:td.first "1-1"]
                               [:td.second "1-2"]])
             item-struct {:name :f
                          :type :simple
                          :finder (enlive-selector [:tr :td.first :> e/text-node])}]
         (fact "with an item structure for col-type returns structured items"
               (extract-attr item-struct example) => {:f "1-1"})
         (fact "with nil finder returns sampled identity"
               (extract-attr (assoc item-struct :finder nil) example) => {:f (h/html [:tr
                                                                                      [:td.first "1-1"]
                                                                                      [:td.second "1-2"]])})))

(facts "about `extract-attr` collection"
       (let [example (h/html [:table
                              [:tr [:td "1"]]
                              [:tr [:td "2"]]
                              [:tr [:td "3"]]])
             col-struct {:name :all
                         :type :collection
                         :col-type nil
                         :limit 10
                         :splitter (enlive-splitter [:table :tr])}]
         (fact "with nil col-type returns colection with unstructured items"
               (extract-attr col-struct example) => {:all (list (h/html [:tr [:td "1"]])
                                                                (h/html [:tr [:td "2"]])
                                                                (h/html [:tr [:td "3"]]))})))

(facts "about `extract`"
       (let [example (h/html [:table
                              [:tr
                               [:td.first "1-1"]
                               [:td.second "1-2"]]
                              [:tr
                               [:td.first "2-1"]
                               [:td.second "2-2"]]
                              [:tr
                               [:td.first "3-1"]
                               [:td.second "3-2"]]])
             item-struct [{:name :f
                           :type :simple
                           :finder (enlive-selector [:tr :td.first :> e/text-node])}
                          {:name :s
                           :type :simple
                           :finder (enlive-selector [:tr :td.second  :> e/text-node])}]
             col-struct [{:name :all
                          :type :collection
                          :col-type item-struct
                          :limit 10
                          :splitter (enlive-splitter [:table :tr])}]]
         (fact "with composed structure"
               (extract col-struct example) => {:all (list {:f "1-1"
                                                            :s "1-2"}
                                                           {:f "2-1"
                                                            :s "2-2"}
                                                           {:f "3-1"
                                                            :s "3-2"})})))
