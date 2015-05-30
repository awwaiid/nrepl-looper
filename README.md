# looper

Experiment with a command-line looper!

# TODO

Turn this into nREPL middleware, similar to
https://github.com/jonase/nrepl-transcript/blob/master/src/nrepl_transcript/file.clj

The idea is to allow anything to connect to this server, and then start a special command that intercepts and records the commands and timing.

It should also have access to the current loop and past loops? hmm.

Then clients connect to this server as their looper, and can do all the things -- record loops, start/stop loops, edit loops. The server itself would be responsible for playing the music for now.

So then we can have a GUI that connects to this, sending the commands and coordinating the overall loops.

