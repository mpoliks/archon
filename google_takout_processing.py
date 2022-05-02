import os, os.path

for root, dirs, files in os.walk("/Users/marekpoliks/Desktop/ARCHON_db/", topdown=False):

   for name in files:

      print(root + name)
      # if (str(name[:-4])) == '5.wav': 
        #  print(str(root) + str(name)[:-6] + "500.wav")