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
               (extract-attr (assoc item-struct :finder nil) example) => {:f (html-sampler
                                                                              (h/html [:tr
                                                                                       [:td.first "1-1"]
                                                                                       [:td.second "1-2"]]))})))

(facts "about `extract-attr` collection"
       (let [page1 (h/html [:div [:table
                                  [:tr [:td "row1"]]
                                  [:tr [:td "row2"]]
                                  [:tr [:td "row3"]]]
                            [:a.next-page {:href "http://2"}]])
             page2 (h/html [:div [:table
                                  [:tr [:td "row2-1"]]
                                  [:tr [:td "row2-2"]]
                                  [:tr [:td "row2-3"]]]])
             paged-col-item [{:name :i
                              :type :simple
                              :finder (xpath-selector "/tr/td/text()")}]
             paged-col-attr {:name :rows
                             :type :collection
                             :col-type paged-col-item
                             :splitter (xpath-splitter "/div/table/tr")
                             :next-page (fn [url html]
                                          ((xpath-selector "/div/a/@href") html))}
             fetch-url-mock (fn [_] page2)
             col-struct {:name :all
                         :type :collection
                         :col-type nil
                         :limit 10
                         :splitter (enlive-splitter [:div :table :tr])}]
         (fact "with nil col-type returns colection with unstructured items"
               (extract-attr col-struct page1) => {:all (list
                                                           (html-sampler (h/html [:tr [:td "row1"]]))
                                                           (html-sampler (h/html [:tr [:td "row2"]]))
                                                           (html-sampler (h/html [:tr [:td "row3"]])))})


         (fact "with paged collection"
               (with-redefs [fetch-url fetch-url-mock]
                 (extract-attr paged-col-attr page1) => {:rows '({:i "row1"}
                                                                 {:i "row2"}
                                                                 {:i "row3"}
                                                                 {:i "row2-1"}
                                                                 {:i "row2-2"}
                                                                 {:i "row2-3"})}))
         (fact "with limited collection"
               (with-redefs [fetch-url fetch-url-mock]
                 (extract-attr (assoc paged-col-attr :limit 4) page1) => {:rows
                                                                          '({:i "row1"}
                                                                            {:i "row2"}
                                                                            {:i "row3"}
                                                                            {:i "row2-1"})}))))

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

(facts "about defentity"
       (fact
        (defentity person
          [name :prop1 val1 :prop2 val2]
          [age :prop1 val1 :prop2 val2])

        =expands-to=>

        (def person [{:name :name, :prop1 val1, :prop2 val2, :type :simple}
                     {:name :age, :prop1 val1, :prop2 val2, :type :simple}])))
