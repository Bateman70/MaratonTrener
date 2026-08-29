import os

build_path = r"c:\Users\Bruker\Android Studio\Maratontrener\app\build"
if os.path.exists(build_path):
    print("Searching for AD_ID in build folder:")
    for root, dirs, files in os.walk(build_path):
        for file in files:
            if file.endswith(".xml"):
                fpath = os.path.join(root, file)
                try:
                    with open(fpath, "r", encoding="utf-8", errors="ignore") as f:
                        content = f.read()
                    if "com.google.android.gms.permission.AD_ID" in content or "permission.AD_ID" in content:
                        print(f"Found in: {fpath}")
                except Exception:
                    pass
else:
    print("Build folder does not exist.")
