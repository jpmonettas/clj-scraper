(ns clj-scraper.core-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [hiccup.core :as h]
            [clj-scraper.core :refer :all]
            [clj-scraper.utils :refer :all]
            [net.cgrand.enlive-html :as e]))


(facts "about get-attr-type"
       (fact "collection"
             (get-attr-type {:name :test :splitter :spl}) => :collection)
       (fact "simple"
             (get-attr-type {:name :test}) => :simple)
       (fact "record"
             (get-attr-type {:name :test :entity :other-ent}) => :record))

(facts "about extract-attr simple"
       (let [example (h/html [:tr
                               [:td.first "11"]
                              [:td.second "12"]])
             page1 (h/html [:tr
                            [:td.nombre "Boby"]
                            [:td.ape "Tables"]
                            [:td [:a {:href "http://2"}]]])
             page2 (h/html [:tr [:td.first "Other Content"]])
             item-attr {:name :f
                        :type :simple
                        :finder (enlive-selector [:tr :td.first :> e/text-node])}]
         (fact "with an item structure for entity returns structured items"
               (extract-attr item-attr example "" ) => {:f "11"})
         (fact "with an item structure formatted"
               (extract-attr (assoc item-attr :formatter #(str % "-test")) example "" ) => {:f "11-test"})
         (fact "with nil finder returns sampled identity"
               (extract-attr (assoc item-attr :finder nil) example "" ) => {:f (html-sampler
                                                                              (h/html [:tr
                                                                                       [:td.first "11"]
                                                                                       [:td.second "12"]]))})
         (fact "with follow"
               (extract-attr (assoc item-attr :follow (fn [u,h]
                                                        (xpath-selector "/tr/td/a/@href") h))
                             page1 "" )

               =>

               {:f "Other Content"}
               (provided (fetch-url anything) => page2))))

(facts "about extract-attr record"
       (let [page1 (h/html [:tr
                            [:td.nombre "Boby"]
                            [:td.ape "Tables"]
                            [:td [:a {:href "http://2"}]]])
             page2 (h/html [:tr
                            [:td.first "Other Content"]])
             item-det-struct [{:name :t
                               :type :simple
                               :finder (enlive-selector [:tr :td.first :> e/text-node])}]
             item-attr {:name :f
                        :type :record
                        :entity item-det-struct
                        :follow (fn [u h]
                                  ((xpath-selector "/tr/td/a/@href") h))}]
         (fact "simple case"
               (extract-attr item-attr page1 "" )  => {:f {:t "Other Content"}}
                 (provided (fetch-url anything) => page2))))



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
                             :entity paged-col-item
                             :splitter (xpath-splitter "/div/table/tr")
                             :next-page (fn [url html]
                                          ((xpath-selector "/div/a/@href") html))}
             col-attr {:name :all
                         :type :collection
                         :entity nil
                         :limit 10
                         :splitter (enlive-splitter [:div :table :tr])}]
         (fact "with nil entity returns colection with unstructured items"
               (extract-attr col-attr page1 "" ) => {:all (list
                                                           (html-sampler (h/html [:tr [:td "row1"]]))
                                                           (html-sampler (h/html [:tr [:td "row2"]]))
                                                           (html-sampler (h/html [:tr [:td "row3"]])))})


         (fact "with paged collection"
               (extract-attr paged-col-attr page1 "" ) => {:rows '({:i "row1"}
                                                                   {:i "row2"}
                                                                   {:i "row3"}
                                                                   {:i "row2-1"}
                                                                   {:i "row2-2"}
                                                                   {:i "row2-3"})}
               (provided (fetch-url anything) => page2))

         (fact "with limited collection"
               (extract-attr (assoc paged-col-attr :limit 4) page1 "" ) => {:rows
                                                                            '({:i "row1"}
                                                                              {:i "row2"}
                                                                              {:i "row3"}
                                                                              {:i "row2-1"})}
               (provided (fetch-url anything) => page2))
         (fact "with next button not found"
               (extract-attr (assoc paged-col-attr :next-page (fn [url html]
                                                                ((xpath-selector "/wrong/a/@href") html)))
                             page1 "" )

               =>

               {:rows
                '({:i "row1"}
                  {:i "row2"}
                  {:i "row3"})})))

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
                          :entity item-struct
                          :limit 10
                          :splitter (enlive-splitter [:table :tr])}]]
         (fact "with composed structure"
               (extract col-struct example "") => {:all (list {:f "1-1"
                                                            :s "1-2"}
                                                           {:f "2-1"
                                                            :s "2-2"}
                                                           {:f "3-1"
                                                            :s "3-2"})})))

(facts "about defentity"
       (fact

        (defentity person
          [first-name :finder "/div[@class='name']/text()"]
          [age :finder "/div[@class='age']/text()"]
          [friends :splitter spl-func :limit 5])

        =expands-to=>

        (def person [{:name :first-name
                      :finder "/div[@class='name']/text()"
                      :type :simple}
                     {:name :age
                      :finder "/div[@class='age']/text()"
                      :type :simple}
                     {:name :friends
                      :type :collection
                      :limit 5 :splitter spl-func}])))
