(defproject nrepl-looper "0.1.0"
  :description "nREPL middleware to replay command with timing, like a music looper"
  :url "https://github.com/awwaiid/nrepl-looper"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.nrepl "0.2.8"]
                 [overtone/at-at "1.2.0"]]
  :repl-options {:nrepl-middleware [nrepl-looper.middleware/wrap-looper]})
