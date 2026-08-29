import re

files = [
    r"c:\Users\Bruker\Android Studio\Maratontrener\web\app.js",
    r"c:\Users\Bruker\Android Studio\Maratontrener\web\index.html"
]

for fpath in files:
    print(f"=== {fpath} ===")
    with open(fpath, "r", encoding="utf-8") as f:
        lines = f.readlines()
    for idx, line in enumerate(lines):
        if "buddies" in line.lower():
            print(f"{idx+1}: {line.strip()}")
