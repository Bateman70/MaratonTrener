import os

for root, dirs, files in os.walk(r"c:\Users\Bruker\Android Studio\Maratontrener\app"):
    for file in files:
        if "race_info" in file:
            print(os.path.join(root, file))
