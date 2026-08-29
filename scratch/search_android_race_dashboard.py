import os

app_path = r"c:\Users\Bruker\Android Studio\Maratontrener\app"
print("=== Java files referencing race info or edit fields ===")
for root, dirs, files in os.walk(app_path):
    for file in files:
        if file.endswith(".java"):
            fpath = os.path.join(root, file)
            with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
            if "currentRace" in content or "editRaceName" in content or "textGoalPace" in content:
                print(f"Java: {fpath}")

print("=== Layout files referencing race details ===")
for root, dirs, files in os.walk(app_path):
    for file in files:
        if file.endswith(".xml"):
            fpath = os.path.join(root, file)
            with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
            if "race" in file.lower() or "goal" in content.lower():
                # Let's check if it has the current goal card fields
                if "progress" in content.lower() or "countdown" in content.lower():
                    print(f"Layout: {fpath}")
