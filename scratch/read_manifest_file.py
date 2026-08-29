import os

manifest_path = r"c:\Users\Bruker\Android Studio\Maratontrener\app\src\main\java\..\..\..\app\src\main\AndroidManifest.xml"
if os.path.exists(manifest_path):
    print("=== Manifest Contents ===")
    with open(manifest_path, "r", encoding="utf-8") as f:
        print(f.read())
else:
    # Try finding it recursively under app/src
    for root, dirs, files in os.walk(r"c:\Users\Bruker\Android Studio\Maratontrener\app"):
        for file in files:
            if file == "AndroidManifest.xml":
                fpath = os.path.join(root, file)
                print(f"=== Manifest: {fpath} ===")
                with open(fpath, "r", encoding="utf-8") as f:
                    print(f.read())
