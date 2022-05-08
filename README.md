# Archon 

## 1. Overview
Archon is an audio data interface written in Python and Supercollider. It mediates between live sound and a feature-extracted audio dataset. Archon is in its early stages - at present, it can extract a base set of descriptors from a database (into JSON), quickly compute input audio against the dataset (via PyTorch), and deliver feature-matched or -excluded database audio into a buffer management system in Supercollider. Future work will significantly build out the Supercollider interface, and will also:
a. Use expanded feature set (MFCCs) 
b. Allow for GPU computation where available 
c. Allow for local extraction and processing (currently using GDrive and Google Colab, which is convenient for collaboration but has many problems in practice)
d.  Coalesce into a complete performance instrument

## 2. Extraction
The '/utilities' folder has many scripts that can be useful to prepare your audio database, from any accessible directory within GDrive. 

a. `archon_split.ipynb` is a Colab notebook that grabs audio from walking through a directory and splices it into homogenous chunks. Since many collaborative directories, especially those used in machine learning, often have lots of duplicates, the `archon_dedupe.ipynb` script uses file hash to trim out any redundant material from your target directory.
b. `archon_analyze.ipynb` has two components. First, a script that extracts median descriptors across the length of each sample (this will ultimately be developed for greater granularity, but for short splices like 500ms it works great). Second, there's an optional script that reorganizes the target directory by descriptor. This is definitely optional, but dealing with GDrive downloading can be a headache without this. (I don't recommend using Google Takeout, which can compromise filenames and do weird things to you data, and instead break up your directories and download them independently with a separate script). There's an unsort argument here you can use to collapse everything back into the original flat directory structure.
c. `archon_post_processing.ipynb` gives you some insights into your data - histograms collected against each major descriptor type (showing pitched data only, unpitched data only, or both for maximum visibility into the contents of your workspace).

The recommended flow is to use the split and analyze scripts to diffract an existing database into many grains, analyze them, sort them, download them, and also download the analysis file (a JSON file) and place it in the directory you cloned this one into. (There's a sample JSON file in this repo, but only just to compare file structure - it won't help you unless you have the files :p). 

## 3. Supercollider Interface 
Once you've downloaded your audio, your analysis file, and this repo, move the three extensions files (ending in .sc - 'archon_classes_local', 'archon_methods_local', `archon_synths_local`) to your Supercollider Extensions directory (`/Library/Application Support/Supercollider/Extensions`). This is an unfortunate requirement of working tidily with Supercollider. Then open up Supercollider, which will auto-compile the class library. Hopefully you will see no errors.

Then poke around `archon_query.py` to get a sense of the arguments available to you, including specifying the location of the analysis file and your audio sample directory. The ports here are auto-specified to work with Supercollider, but if you see a mismatch when you boot `archon_brain.scd` you can easily make an adjustment by passing the `--output_port` argument with the relevant port that SC prints out for you.

To boot the SC script, just click anywhere within the SC editor and hit CMD+Enter (Mac). It will take a few seconds to load. You'll see `OK: Ready!` in the post window when it's time to proceed.

After it's ready, open up your command line editor, navigate to the directory where you cloned this repo, and run `python archon_query.py`. You may need to install torch/numpy/pandas/python_osc depending on your build (I'll have a script grab these in the future). This query can take a few seconds to minutes to load, depending on the size of your analysis file. It'll let you know it's serving on the localhost once you're good to go.

Make sure your headphones are plugged in and make some sound! Play around with the PKill function in the `archon_classes.sc` extension file for maximum fun. Stay tuned, this will grow quite a bit and I'll keep the readme up to date.