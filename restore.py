import json
import re
import os

out_file = r'c:\Users\Bruker\Android Studio\Maratontrener\app\src\main\res\layout\activity_start.xml'
transcript_file = r'C:\Users\Bruker\.gemini\antigravity\brain\f4d76941-a17f-439f-8476-8ebd444d064f\.system_generated\logs\transcript_full.jsonl'

with open(transcript_file, 'r', encoding='utf-8') as f:
    for line in f:
        data = json.loads(line)
        if 'tool_calls' in data:
            for call in data['tool_calls']:
                if 'output' in call:
                    out = call['output']
                    if 'activity_start.xml' in out and '384' in out and '<?xml version=\"1.0\"' in out:
                        start_idx = out.find('1: <?xml')
                        if start_idx != -1:
                            content = out[start_idx:]
                            # Clean up line numbers and other viewer artifacts
                            clean_content = re.sub(r'^\d+:\s', '', content, flags=re.MULTILINE)
                            clean_content = clean_content.replace('The above content shows the entire, complete file contents of the requested file.', '')
                            with open(out_file, 'w', encoding='utf-8') as out_f:
                                out_f.write(clean_content.strip())
                                print('Successfully restored activity_start.xml')
                            exit(0)
print('Could not find it in transcript_full.jsonl')
