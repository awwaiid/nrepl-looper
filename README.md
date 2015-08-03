# nrepl-looper

A funky looper pedal... for a REPL. In this case nREPL (ala Clojure). The idea is to use this with Overtone. Unlike other loopers in which a loop is opaque, here each loop is a data structure with individual events that can be manipulated while the loop is running.

# Loop Data Structure

```clojure
(def loop-1

  "Example datastructure for a loop
  It is an atom so that it can be modified during playback.

  length: How long the loop goes. If events happen after this then they will be
  mod'ed to fit the timeline

  start-time: Used during recording to know the offset of a 'now' event

  events: vector of events

  offset: offset is milliseconds from the start of the loop
  cmd: a string that will be eval'd"

  (atom
    {:length 4000 ; Events after this will be mod'ed
     :start-time 1426700231396 ; unix milliseconds
     :events [{:offset 0    :cmd "(piano 60)"}
              {:offset 1000 :cmd "(piano 60)"}
              {:offset 2000 :cmd "(piano 63)"}
              {:offset 3000 :cmd "(piano 65)"}]}))
```

# nREPL Middleware

The current incarnation of this is some middleware for nREPL. It's a bit mind-twisty, but the idea is that you're using your repl like normal, but can issue some commands that get intercepted by the middleware. One command is to start recording, another to stop, and another to play a loop. During recording the middleware captures all the stuff you do, including timing.

# TODO / NOTES

* Simplify the middleware in any way possible
* Visualize/Manipulate the loops
    * Maybe a cool web interface with touch screen to manipulate
* Right now all is based on millisecond time offsets -- maybe offer beats/bars
    * This might make quantizing easy


