with open(r"c:\Users\Bruker\Android Studio\Maratontrener\app\src\main\java\com\jostein\maratontrener\DashboardFragment.java", "r", encoding="utf-8") as f:
    lines = f.readlines()

for idx, line in enumerate(lines):
    if "homerace" in line.lower() or "raceoverview" in line.lower() or "loadprogress" in line.lower():
        print(f"{idx+1}: {line.strip()}")
