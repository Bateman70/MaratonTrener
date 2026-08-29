import sys

pattern = sys.argv[1]
fpath = r"c:\Users\Bruker\Android Studio\Maratontrener\web\style.css"

print(f"Searching in {fpath} for '{pattern}':")
with open(fpath, "r", encoding="utf-8") as f:
    lines = f.readlines()

for idx, line in enumerate(lines):
    if pattern in line:
        print(f"{idx+1}: {line.strip()}")
