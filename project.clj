(defproject clj-scraper "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.5.1"]

                 ;; Debugging
                 [org.clojure/tools.trace "0.7.6"]

                 ;; HTML parsing
                 [enlive "1.1.5"]

                 ;; Examples
                 [org.marianoguerra/clj-rhino "0.2.1"]

                 ;; XPath
                 [com.github.kyleburton/clj-xpath "1.4.3"]]
  :profiles {:dev {:dependencies
                   ;; Testing
                   [[midje "1.6.2"]
                    [hiccup "1.0.5"]]}}
  :main clj-scraper.core)
