import os

path = r"c:\Users\Bruker\Android Studio\Maratontrener"
while True:
    git_path = os.path.join(path, ".git")
    if os.path.exists(git_path):
        print(f"Found .git at: {path}")
        break
    parent = os.path.dirname(path)
    if parent == path:
        print("No .git directory found in parent tree.")
        break
    path = parent
