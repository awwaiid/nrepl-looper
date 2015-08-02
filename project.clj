(defproject nrepl-looper "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.nrepl "0.2.8"]
                 [overtone/at-at "1.2.0"]]
  :jvm-opts ^:replace []
  :repl-options {:nrepl-middleware
                 [nrepl-looper.middleware/wrap-looper]})
