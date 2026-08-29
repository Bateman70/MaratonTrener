with open(r"c:\Users\Bruker\Android Studio\Maratontrener\web\app.js", "r", encoding="utf-8") as f:
    lines = f.readlines()

print("Search for DOMContentLoaded or window.onload:")
for idx, line in enumerate(lines):
    if "domcontentloaded" in line.lower() or "onload" in line.lower() or "initialize" in line.lower():
        print(f"{idx+1}: {line.strip()}")

print("\nLast 30 lines of app.js:")
for idx, line in enumerate(lines[-30:]):
    print(f"{len(lines)-30+idx+1}: {line.strip()}")
