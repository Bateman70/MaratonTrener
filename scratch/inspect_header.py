import os

filepath = r"c:\Users\Bruker\Android Studio\Maratontrener\app_icon.png"

if os.path.exists(filepath):
    with open(filepath, "rb") as f:
        header = f.read(16)
        print("Header bytes (hex):", header.hex())
        print("Header bytes (ASCII):", header)
else:
    print("File not found.")
