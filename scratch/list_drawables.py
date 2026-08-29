import os

drawable_path = r"c:\Users\Bruker\Android Studio\Maratontrener\app\src\main\res\drawable"
for file in os.listdir(drawable_path):
    if "sun" in file.lower() or "cloud" in file.lower() or "rain" in file.lower() or "weather" in file.lower() or "snow" in file.lower():
        print(file)
