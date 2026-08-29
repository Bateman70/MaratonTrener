with open(r"c:\Users\Bruker\Android Studio\Maratontrener\web\app.js", "r", encoding="utf-8") as f:
    lines = f.readlines()

for idx, line in enumerate(lines):
    if "appstate.buddies" in line.lower():
        print(f"{idx+1}: {line.strip()}")
