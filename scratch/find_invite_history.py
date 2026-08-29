import json

log_path = r"C:\Users\Bruker\.gemini\antigravity\brain\f4d76941-a17f-439f-8476-8ebd444d064f\.system_generated\logs\transcript.jsonl"

print("Searching transcript for alternative share_body_template values...")
with open(log_path, 'r', encoding='utf-8') as f:
    for line in f:
        try:
            step = json.loads(line)
            content = step.get('content', '')
            if not content:
                continue
            if 'share_body_template' in content:
                for l in content.split('\n'):
                    if 'share_body_template' in l and 'Join me on Maratontrener' not in l:
                        print(f"Step {step.get('step_index')}: {l.strip()[:180]}")
        except Exception as e:
            pass
