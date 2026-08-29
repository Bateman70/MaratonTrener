import os

file_path = r'c:\Users\Bruker\Android Studio\Maratontrener\app\src\main\java\com\jostein\maratontrener\CreatePlanActivity.java'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace the class definition body
start_token = "public class CreatePlanActivity extends AppCompatActivity {"
new_start = """public class CreatePlanActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }
    /*
"""

if start_token in content:
    content = content.replace(start_token, new_start, 1)
    # Append */ to the end of the file before the last brace
    content = content[:content.rfind('}')] + "    */\n}\n"
    
    with open(file_path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("Successfully commented out CreatePlanActivity")
else:
    print("Could not find class definition")
