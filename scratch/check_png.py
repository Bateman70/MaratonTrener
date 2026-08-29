import os
import struct

filepath = r"c:\Users\Bruker\Android Studio\Maratontrener\app_icon.png"

if os.path.exists(filepath):
    print(f"File size: {os.path.getsize(filepath)} bytes")
    # Read PNG dimensions from header
    with open(filepath, "rb") as f:
        header = f.read(24)
        if header.startswith(b"\x89PNG\r\n\x1a\n"):
            # PNG dimensions are in the IHDR chunk
            w, h = struct.unpack(">II", header[16:24])
            print(f"Dimensions: {w}x{h} px")
        else:
            print("Not a valid PNG file.")
else:
    print("File not found.")
