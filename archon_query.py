import string
from turtle import ycor
from scipy.spatial.distance import pdist
import numpy as np
import json as json
import os, os.path
import argparse
import math
import pythonosc

from pythonosc import dispatcher
from pythonosc import osc_server
from pythonosc.udp_client import SimpleUDPClient



def json_load (filename):
  f = open(filename)
  l = json.load(f)
  return l

def closest_node(in_, db_):

    flatscl = 100000.0
    min_ = "init"
    result_ = "error"

    for k, v in in_.items():

      sample = v

      pitch = str(sample.get("pitch"))

      in_metrics = [
              float(
                  sample.get("cent")), 
              float(
                  sample.get("flat")) * flatscl, 
              float(
                  sample.get("rolloff"))]


    for k, v in db_.items():

        sample_ = v
        if (pitch == sample_.get("pitch")):

          db_metrics = [
                float(
                    sample_.get("cent")), 
                float(
                    sample_.get("flat")) * flatscl, 
                float(
                    sample_.get("rolloff"))]
          dist = pdist([in_metrics, db_metrics], metric = 'euclidean')[0]

          if (min_ == "init"): min_ = dist
          if (dist < min_):
            min_ = dist
            result_ = ("distance: " + str(dist) + ", file: " + k)
    
    return result_

def osc_handler(unused_addr, constants, args):
  incoming = json.loads(args)
  node = closest_node(incoming, constants[0])
  constants[1].send_message("/node", node)
  print(node)

if __name__ == "__main__":
  parser = argparse.ArgumentParser()
  parser.add_argument("--ip",
      default="127.0.0.1", help="The ip to listen on")
  parser.add_argument("--in_port",
      type=int, default=5005, help="The port to listen on")
  parser.add_argument("--out_port",
      type=int, default=57120, help="The port to send to")      
  parser.add_argument("--file",
      default="/Users/marekpoliks/Desktop/ARCHON/analysis_500ms.json", help="Location of analysis file (json)")
  args = parser.parse_args()
  
  client = SimpleUDPClient(args.ip, args.out_port)
  client.send_message("/superInterface", "Testing Connection w/ Supercollider")

  dispatcher = dispatcher.Dispatcher()

  dispatcher.map("/test", osc_handler, json_load(args.file), client)

  server = osc_server.ThreadingOSCUDPServer(
      (args.ip, args.in_port), dispatcher)

  print("Serving on {}".format(server.server_address))

  server.serve_forever()
