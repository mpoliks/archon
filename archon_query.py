from multiprocessing.connection import Client
import numpy as np
import json as json
import os, os.path
import argparse
import torch
import pandas as pd

from turtle import ycor
from pythonosc import dispatcher, osc_server
from pythonosc.udp_client import SimpleUDPClient

# simple function to take json storage and convert to pandas df
def dataframe_from_file(filename):
    with open(filename) as t:
        data = json.load(t)
        df = pd.DataFrame(data).T
    database_as_df = dict(tuple((df.groupby('pitch'))))
    return database_as_df

# simple function to convert pandas df into a tensor
def tensor_dict_from_dataframe(database_as_df):
    tens_dict = {}
    for k,v in database_as_df.items():
        v_nopitch = v.drop(labels = 'pitch', axis = 1).drop(labels='rms', axis=1).drop(labels='flat', axis=1)
        column_list = list(v_nopitch.columns)
        tens_dict[k] = torch.tensor(
            v_nopitch.loc[:, column_list]
            .to_numpy(dtype=np.float32))
    return tens_dict

# processes information from supercollider into tensor
def process_input(input_dict):
    print(input_dict)
    pitch = list(input_dict.items())[0][1].get("pitch")
    input_as_df = pd.DataFrame(input_dict).T
    input_as_df = input_as_df.drop(labels='pitch', axis=1).drop(labels='rms', axis=1).drop(labels='flat', axis=1)
    column_list = list(input_as_df.columns)
    in_t = torch.tensor(
        input_as_df.loc[:, column_list]
        .to_numpy(dtype=np.float32))
    return pitch, in_t

# use pytorch to find min. minkowski distance between input and db, return 25 closest matches
# note cuda is not enabled here yet (this is being tested on a 2015 macbook pro with only about 100 ms latency)
def closest_node(input_dataframe, database_dataframe, database_tensors, audiodir):
    pitch, input_tensor = process_input(input_dataframe)
    if pitch not in database_dataframe: pitch = "unpitched"
    this_database_tensor = database_tensors.get(pitch)
    result = []
    dist = torch.cdist(input_tensor, this_database_tensor, p=2).flatten() 
    for i in range(25):
        values, indices = torch.kthvalue(dist, i + 1)
        result.append(
            format_result(
                database_dataframe.get(pitch).iloc[indices.item()], 
                pitch, 
                audiodir))
    return result

# tidy up result so it matches google drive's horrible storage mandates
def format_result(sample, pitch, target_audiodir):
    audiofile = sample.name
    #Takeout-safe formatting only
    #if audiofile[:-6] != 'ms.wav': 
        #audiofile = audiofile[:-4] + "ms.wav"
    if (pitch != "unpitched"): 
      oct = int(pitch[-1])
      pitch = pitch.replace(str(oct), "")  
      return_dir = target_audiodir + pitch + "/" + str(oct) + "/"   
    else: 
      return_dir = target_audiodir + pitch + "/"
      cent = sample.get("cent")
      flat = sample.get("flat")
      rolloff = sample.get("rolloff")
      if float(cent) > 4000.0: return_dir = return_dir + "high_cent/"
      else: return_dir = return_dir + "low_cent/"
      if float(flat) > 0.01: return_dir = return_dir + "high_flat/"
      else: return_dir = return_dir + "low_flat/"
      if float(rolloff) > 8000: return_dir = return_dir+ "high_rolloff/"
      else: return_dir = return_dir + "low_rollof/"  
    return return_dir + audiofile

# just to organize the osc server sequence - osc comes in from SC and the following sequence occurs
def osc_handler(unused_addr, constants, args):
    database_dataframe, database_tensors, audiodir, client = constants[0], constants[1], constants[2], constants[3]
    incoming = json.loads(args)
    node = closest_node(incoming, database_dataframe, database_tensors, audiodir)
    client.send_message("/node", node)
    print(node)

if __name__ == "__main__":

    # IMPORTANT: please read for information around file locations as yours will differ
    parser = argparse.ArgumentParser()
    parser.add_argument("--ip",
        default="127.0.0.1", help="The ip to listen on")
    parser.add_argument("--in_port",
        type=int, default=5005, help="The port to listen on")
    parser.add_argument("--out_port",
        type=int, default=57120, help="The port to send to")      
    parser.add_argument("--file",
        default="/Users/marekpoliks/Desktop/ARCHON_vox/analysis_desilenced.json", help="Location of analysis file (json)")
    parser.add_argument("--audiodb",
        default="/Users/marekpoliks/Desktop/ARCHON_vox/", help="Location of audio database")
    args = parser.parse_args()

    # sets up Supercollider client and test message, if you don't see one in SC, start investigating
    client = SimpleUDPClient(args.ip, args.out_port)
    client.send_message("/superInterface", "ACK: Testing Connection w/ Python Server, Hold for OK")

    # load up existing database and convert to tensors
    print("OK: loading database... this can take a minute if the database is huge!")
    database_dataframe = dataframe_from_file(args.file)
    print("OK: complete!")
    print("OK: loading tensors...")
    database_tensors = tensor_dict_from_dataframe(database_dataframe)
    print("OK: complete!")

    # sets up Supercollider server and awaits OSC messages to /query
    dispatcher = dispatcher.Dispatcher()
    dispatcher.map("/query", osc_handler, database_dataframe, database_tensors, args.audiodb, client)
    server = osc_server.ThreadingOSCUDPServer(
        (args.ip, args.in_port), dispatcher)
    print("Serving on {}".format(server.server_address))
    client.send_message("/superInterface", "OK: Python Server is Ready!")
    server.serve_forever()