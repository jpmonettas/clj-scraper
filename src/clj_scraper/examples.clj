(ns clj-scraper.examples
  (:require [clj-scraper.core :refer :all]
            [clj-scraper.utils :refer :all]
            [net.cgrand.enlive-html :as e]
            [clj-rhino :as js])
  (:use clojure.tools.trace)
  (:gen-class))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; "http://twitaholic.com/"
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment

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


)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; "http://www.cuti.org.uy/socios.html"
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn find-cuti-link [url ctx-h]
  (let [u (str url ((xpath-selector "/tr/td[2]/a/@href") ctx-h))]
    u))


(defn find-email-from-js [js]
  (try
    (let [v-name (nth  (re-matches #"(?is)var (addy.*?) =.*" js) 1)
          scope (js/new-safe-scope)]
      (js/eval scope js)
      (first (e/html-snippet (str (get scope v-name)))))
    (catch Exception e (.printStackTrace e))))

(comment

  (defentity cuti-contact-details
    [email :finder (comp
                    find-email-from-js
              (regexp-selector #"(?is).*(var add.*?)document.write.*" 1)
              (enlive-selector [:table.contentpaneopen]))]
    [dir :finder (comp
              (regexp-selector #"(?is).*Direcci.n: (.+?)<.*" 1)
              (enlive-selector [:table.contentpaneopen]))]
    [tel :finder (comp
              (regexp-selector #"(?is).*Tel.fono: (.+?)<.*" 1)
              (enlive-selector [:table.contentpaneopen]))])

  (defentity cuti-contact
    [name :finder (comp
                   (cleaner \newline \tab)
                   (xpath-selector "/tr/td[2]/a/text()"))]
    [web :follow find-cuti-link
     :finder (comp
              (regexp-selector #"(?is).*\"(http://.+?)\".*" 1)
              (enlive-selector [:table.contentpaneopen]))]
    [details :entity cuti-contact-details :follow find-cuti-link])

    (defentity cuti-contacts
      [contacts :splitter (fn [h]
                            (concat ((enlive-splitter [:tr.sectiontableentry1]) h)
                                    ((enlive-splitter [:tr.sectiontableentry2]) h)))
       :limit 5
       :entity cuti-contact])

      (def cuti-full-page (fetch-url "http://www.cuti.org.uy/socios.html?limit=0"))
  (clojure.pprint/pprint (extract cuti-contacts cuti-full-page "http://www.cuti.org.uy"))

    ;;--------------------------------------------------------------------------------


    (defentity cuti-contact
      [name]
      [web]
      [mail])

    (defentity cuti-contacts
      [contacts
       :splitter (enlive-splitter [[:tr (e/attr-starts "sectiontableentry")]])
       :limit 5
       :entity cuti-contact]
      [title
       :finder (comp
                (cleaner \newline \tab)
                (enlive-selector [:div.componentheading :> e/text-node]))])





  ;; (defentity cuti-contact
  ;;   [name :finder (comp
  ;;                  (cleaner \newline \tab)
  ;;                  (xpath-selector "/tr/td[2]/a/text()"))]
  ;;   [web :follow find-cuti-link
  ;;    :finder (comp
  ;;             (regexp-selector #"(?is).*\"(http://.+?)\".*" 1)
  ;;             (enlive-selector [:table.contentpaneopen]))]
  ;;   [email :follow find-cuti-link
  ;;    :finder (comp
  ;;             find-email-from-js
  ;;             (regexp-selector #"(?is).*(var add.*?)document.write.*" 1)
  ;;             (enlive-selector [:table.contentpaneopen]))]
  ;;   [dir :follow find-cuti-link
  ;;    :finder (comp
  ;;             (regexp-selector #"(?is).*Direcci.n: (.+?)<.*" 1)
  ;;             (enlive-selector [:table.contentpaneopen]))]
  ;;   [tel :follow find-cuti-link
  ;;    :finder (comp
  ;;             (regexp-selector #"(?is).*Tel.fono: (.+?)<.*" 1)
  ;;             (enlive-selector [:table.contentpaneopen]))])


)


(comment

  (defentity detalles
    [sitio]
    [email])


  (defentity socio
    [nombre :follow link-finder :finder find]
    [det :entity detalles :follow link-finder-2])


  (defentity socios-cuti
    [socios :splitter spl :limit 10 :entity socio])


  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; http://www.blacklionmusic.com/lista-de-salsa/ ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(comment
  
(defentity album
  [album-name :finder (xpath-selector "/a/text()")])

(defentity albums
  [all-albums :splitter (enlive-splitter [:div.td-post-content :> :p :> :strong :> :a])
   ;; :limit 5
   :entity album])

(defentity artist
  [artist-name :finder (comp 
                        (regexp-selector #"(.+?) \[Discografia\].*" 1)
                        (xpath-selector "/a/text()"))]
  [artist-albums :follow (fn [_ h] ((xpath-selector "/a/@href") h)) :entity albums] )

(defentity artists
  [artists :splitter (enlive-splitter [:div.td-page-content :a])
   ;; :limit 10
   :entity artist])


(spit "/home/jmonetta/mambolist.clj"
      (with-out-str
        (clojure.pprint/pprint
         (extract-url artists "http://www.blacklionmusic.com/lista-de-salsa/"))))


)
