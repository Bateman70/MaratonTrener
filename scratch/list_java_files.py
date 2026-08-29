import os

pkg_path = r"c:\Users\Bruker\Android Studio\Maratontrener\app\src\main\java\com\jostein\maratontrener"
for file in os.listdir(pkg_path):
    if file.endswith(".java"):
        print(file)
