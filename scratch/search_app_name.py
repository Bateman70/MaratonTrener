with open(r"c:\Users\Bruker\Android Studio\Maratontrener\app\src\main\res\values\strings.xml", "r", encoding="utf-8") as f:
    lines = f.readlines()

for idx, line in enumerate(lines):
    if 'name="app_name"' in line:
        print(f"{idx+1}: {line.strip()}")
