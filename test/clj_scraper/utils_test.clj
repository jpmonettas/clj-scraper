(ns clj-scraper.utils-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [hiccup.core :as h]
            [clj-scraper.utils :refer :all]
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
                                                                             (h/html [:tr [:td 3]])))
         (fact "with &"
               ((xpath-splitter "/table/tr") "<table><tr><td>&</td></tr></table>"))))

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

(comment
  (facts "about html-sample"
         (fact
          (html-sample (repeat (- html-sampler-size 10))) => "This is a long strin...")
         (fact
          (html-sample "This is ok") => "This is a long strin...")
         (fact
          (html-sample "This is a long string") => "This is a long strin...")))
