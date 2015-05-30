(ns looper.patch-repl)

(ns reply.eval-modes.standalone)

(def execute
  (looper.core/wrap-repl-execute execute))

