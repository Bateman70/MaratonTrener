import shutil
import os

src = r"C:\Users\Bruker\.gemini\antigravity\brain\f4d76941-a17f-439f-8476-8ebd444d064f\marathon_trainer_icon_shoe_1781904881805.png"
dst = r"c:\Users\Bruker\Android Studio\Maratontrener\app_icon.png"

if os.path.exists(src):
    shutil.copy(src, dst)
    print(f"Successfully copied app icon to: {dst}")
else:
    print("Source image not found.")
