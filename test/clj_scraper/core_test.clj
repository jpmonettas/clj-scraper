(ns clj-scraper.core-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [hiccup.core :as h]
            [clj-scraper.core :refer :all]
            [net.cgrand.enlive-html :as e]))

(facts "about xpath-splitter"
       (let [example (h/html [:table
                              [:tr [:td 1]]
                              [:tr [:td 2]]
                              [:tr [:td 3]]])]
         (fact "with table->tr"
               ((xpath-splitter "/table/tr") example) => (list (h/html [:tr [:td 1]])
                                                               (h/html [:tr [:td 2]])
                                                               (h/html [:tr [:td 3]])))
         (fact "with table->tr[position()>1]"
               ((xpath-splitter "/table/tr[position()>1]") example) => (list (h/html [:tr [:td 2]])
                                                                             (h/html [:tr [:td 3]])))))

(facts "about xpath-selector"
       (let [example (h/html [:table
                              [:tr [:td 1]]])]
         (fact "with table->tr->td->node-text"
               ((xpath-selector "/table/tr/td/text()") example) => "1")))

(facts "about enlive-selector"
       (let [example (h/html [:tr
                              [:td.test 5]
                              [:td 6]])
             sel1 (enlive-selector [:tr :td.test])
             sel2 (enlive-selector [:tr :td.test :> e/text-node])]
         (fact "with entire node selection"
               (sel1 example) => (h/html [:td.test 5]))
         (fact "with text-node selection"
               (sel2 example) => "5")))

(facts "about enlive-splitter"
       (let [example (h/html [:table
                              [:tr [:td 1]]
                              [:tr [:td 2]]
                              [:tr [:td 3]]])
             spl1 (enlive-splitter [:table :tr])]
         (fact "with table->tr->td->val"
               (spl1 example) => (list (h/html [:tr [:td 1]])
                                       (h/html [:tr [:td 2]])
                                       (h/html [:tr [:td 3]])))))

(facts "about extract-attr simple"
       (let [example (h/html [:tr
                               [:td.first "1-1"]
                               [:td.second "1-2"]])
             item-struct {:name :f
                          :type :simple
                          :finder (enlive-selector [:tr :td.first :> e/text-node])}]
         (fact "with an item structure for col-type returns structured items"
               (extract-attr item-struct example) => {:f "1-1"})
         (fact "with nil finder returns identity"
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
