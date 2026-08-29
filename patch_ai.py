import os
import re

file_path = r'c:\Users\Bruker\Android Studio\Maratontrener\web\app.js'

with open(file_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Add elements
if "importLoadingSpinner: document.getElementById('import-loading-spinner')" not in content:
    content = content.replace(
        "importFileInput: document.getElementById('import-file-input'),",
        "importFileInput: document.getElementById('import-file-input'),\n        inputGeminiApiKey: document.getElementById('input-gemini-api-key'),\n        importLoadingSpinner: document.getElementById('import-loading-spinner'),"
    )

# 2. Add API key load/save logic to initialization
if "elements.inputGeminiApiKey.value = localStorage.getItem('geminiApiKey') || '';" not in content:
    init_hook = "elements.btnCloseWizard.addEventListener('click', closeWizardModal);"
    api_key_logic = """
    // Load API Key
    if (elements.inputGeminiApiKey) {
        elements.inputGeminiApiKey.value = localStorage.getItem('geminiApiKey') || '';
        elements.inputGeminiApiKey.addEventListener('input', (e) => {
            localStorage.setItem('geminiApiKey', e.target.value.trim());
        });
    }
    """
    content = content.replace(init_hook, api_key_logic + "\n    " + init_hook)

# 3. Rewrite handleFileImport and add Gemini logic
new_import_logic = """
async function handleFileImport(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    // Hide previous UI states
    elements.importPreviewContainer.style.display = 'none';
    elements.btnWizImportFinish.disabled = true;
    
    if (file.name.endsWith('.json') || file.name.endsWith('.csv')) {
        // Local Parsing
        const reader = new FileReader();
        reader.onload = function(e) {
            try {
                const contents = e.target.result;
                let parsedWorkouts = [];
                if (file.name.endsWith('.json')) parsedWorkouts = JSON.parse(contents);
                else parsedWorkouts = parseCSV(contents);
                
                if (!Array.isArray(parsedWorkouts) || parsedWorkouts.length === 0) {
                    alert('No valid workouts found.');
                    return;
                }
                
                window.pendingImportPlan = parsedWorkouts;
                renderImportPreview(parsedWorkouts);
                elements.btnWizImportFinish.disabled = false;
                elements.importPreviewContainer.style.display = 'block';
            } catch (err) {
                alert('Error parsing file: ' + err.message);
            }
        };
        reader.readAsText(file);
    } else {
        // AI Parsing (PDF / Images)
        const apiKey = localStorage.getItem('geminiApiKey');
        if (!apiKey) {
            alert('Please enter your Gemini API Key first.');
            return;
        }
        
        elements.importUploadZone.style.display = 'none';
        elements.importLoadingSpinner.style.display = 'block';
        
        try {
            const base64Data = await fileToBase64(file);
            const mimeType = file.type;
            
            const aiResponse = await callGeminiAPI(base64Data, mimeType, apiKey);
            
            if (!aiResponse || !Array.isArray(aiResponse) || aiResponse.length === 0) {
                throw new Error("AI returned invalid plan format.");
            }
            
            window.pendingImportPlan = aiResponse;
            renderImportPreview(aiResponse);
            elements.btnWizImportFinish.disabled = false;
            elements.importPreviewContainer.style.display = 'block';
            
        } catch (err) {
            console.error('AI Processing Error:', err);
            alert('AI Processing failed: ' + err.message);
        } finally {
            elements.importUploadZone.style.display = 'block';
            elements.importLoadingSpinner.style.display = 'none';
        }
    }
}

function fileToBase64(file) {
    return new Promise((resolve, reject) => {
        const reader = new FileReader();
        reader.readAsDataURL(file);
        reader.onload = () => {
            // Remove the data:image/png;base64, prefix
            const result = reader.result.split(',')[1];
            resolve(result);
        };
        reader.onerror = error => reject(error);
    });
}

async function callGeminiAPI(base64Data, mimeType, apiKey) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=${apiKey}`;
    
    // Dynamically get the race type from Step 1 so the AI acts accordingly
    const targetDistance = elements.wizRaceType ? elements.wizRaceType.value : "Marathon";
    
    const prompt = `You are an expert ${targetDistance} running coach. Analyze the provided training plan image/PDF.
Extract all workouts into a strict JSON array. Each object in the array MUST have these exact keys:
"date": string (YYYY-MM-DD) - Calculate dates assuming the plan ends on the race date provided below. If no dates are visible, assume Day 1 is today (${new Date().toISOString().split('T')[0]}).
"type": string (e.g. 'LONG RUN', 'INTERVALS', 'EASY', 'REST')
"distance": number (Extract total distance in kilometers. If it says miles, convert to km. If it's a time-based run, estimate distance assuming 6:00 min/km pace, or just put 0)
"title": string (A short name like '8x400m Intervals' or '15km Long Run')
"description": string (Any specific pacing or instructions visible)

Return ONLY valid JSON. No markdown formatting, no backticks.
[
  {"date": "2024-01-01", "type": "EASY", "distance": 5, "title": "Recovery", "description": "Keep HR low"}
]`;

    const body = {
        "contents": [{
            "parts": [
                {"text": prompt},
                {
                    "inline_data": {
                        "mime_type": mimeType,
                        "data": base64Data
                    }
                }
            ]
        }]
    };

    const response = await fetch(url, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(body)
    });

    if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Gemini API Error: ${response.status} - ${errorText}`);
    }

    const data = await response.json();
    let textResponse = data.candidates[0].content.parts[0].text;
    
    // Clean up potential markdown formatting
    textResponse = textResponse.replace(/```json/g, '').replace(/```/g, '').trim();
    
    return JSON.parse(textResponse);
}
"""

# Replace the old handleFileImport completely
content = re.sub(r'function handleFileImport\(event\) \{.*?\n\}\n\nfunction parseCSV', new_import_logic + '\n\nfunction parseCSV', content, flags=re.DOTALL)

with open(file_path, 'w', encoding='utf-8') as f:
    f.write(content)

print("Updated app.js with AI logic")
