import numpy as np
import json as json
import os, os.path
import argparse
import math
import pythonosc

from pythonosc import dispatcher
from pythonosc import osc_server


def print_handler(unused_addr, args, json_):
  #print("[{0}] ~ {1}".format(args[0], volume))
  print(args)
  incoming = json.loads(json_)
  print(incoming)


def print_compute_handler(unused_addr, args, volume):
  try:
    print("[{0}] ~ {1}".format(args[0], args[1](volume)))
  except ValueError: pass

if __name__ == "__main__":
  parser = argparse.ArgumentParser()
  parser.add_argument("--ip",
      default="127.0.0.1", help="The ip to listen on")
  parser.add_argument("--port",
      type=int, default=5005, help="The port to listen on")
  args = parser.parse_args()

  dispatcher = dispatcher.Dispatcher()
  dispatcher.map("/test", print_handler)

  server = osc_server.ThreadingOSCUDPServer(
      (args.ip, args.port), dispatcher)
  print("Serving on {}".format(server.server_address))
  server.serve_forever()
