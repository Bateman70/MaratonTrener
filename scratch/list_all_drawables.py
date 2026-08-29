import os

drawable_path = r"c:\Users\Bruker\Android Studio\Maratontrener\app\src\main\res\drawable"
for file in sorted(os.listdir(drawable_path)):
    print(file)
