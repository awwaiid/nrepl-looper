(defproject looper "0.1.0-SNAPSHOT"
  :dependencies [
    [org.clojure/clojure "1.6.0"]
    [org.clojure/core.async "0.1.338.0-5c5012-alpha"]
    [overtone "0.9.1"]
    [jline "2.11"]
    [ruiyun/tools.timer "1.0.1"]
    [org.clojure/tools.nrepl "0.2.8"]
    [debugger "0.1.7"]
  ]
  :jvm-opts ^:replace []
  :repl-options {:nrepl-middleware
                 [looper.middleware/looper-wrapper]}
  ; :main looper.core
)
