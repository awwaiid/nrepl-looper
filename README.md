# nrepl-looper

A funky looper pedal... for a REPL. In this case nREPL (ala Clojure). The idea is to use this with Overtone. Unlike other loopers in which a loop is opaque, here each loop is a data structure with individual events that can be manipulated while the loop is running.

# Usage example

Make a new project: lein new app nrepl-looper-demo

Edit the project.clj, adding the looper and overtone dependencies, and enable the middleware:

```clojure
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [overtone "0.9.1"]
                 [nrepl-looper "0.1.0"]]
  :repl-options {:nrepl-middleware [nrepl-looper.middleware/wrap-looper]}
```

Start your REPL via ```lein repl```, and then do something like this to get set up:

```
(use 'overtone.live)
(use 'overtone.inst.piano)
(def loop1 (nrepl-looper.middleware/empty-loop))
```

Now let's record a loop:
```
(looper "record" loop1)
(piano 60)
(piano 62)
(piano 63)
(looper "stop-recording" loop1)
```

That will give you something like:
```
nrepl-looper-demo.core=> (pprint @loop1)
{:events
 [{:offset 3438, :cmd "(piano 60\n)"}
  {:offset 7082, :cmd "(piano 62)"}
  {:offset 10142, :cmd "(piano 63)"}],
 :start-time 1440343470500,
 :length 15744}
```

Finally, try:
```
(looper "play" loop1)
```

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
    {:length 4000 ; Events after this will be mod'ed to wrap
     :start-time 1426700231396 ; Internal. Unix milliseconds, set on rec/play
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
    * :unit "beats", :bpm 100; :unit "ms"


