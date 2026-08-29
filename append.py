import os

code = """
// ----------------------------------------------------
// EXTERNAL PLAN IMPORT LOGIC
// ----------------------------------------------------

function handleFileImport(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    const reader = new FileReader();
    reader.onload = function(e) {
        try {
            const contents = e.target.result;
            let parsedWorkouts = [];
            
            if (file.name.endsWith('.json')) {
                parsedWorkouts = JSON.parse(contents);
            } else if (file.name.endsWith('.csv')) {
                parsedWorkouts = parseCSV(contents);
            } else {
                alert('Unsupported file format. Please upload .csv or .json');
                return;
            }
            
            if (!Array.isArray(parsedWorkouts) || parsedWorkouts.length === 0) {
                alert('No valid workouts found in the file.');
                return;
            }
            
            window.pendingImportPlan = parsedWorkouts;
            renderImportPreview(parsedWorkouts);
            elements.btnWizImportFinish.disabled = false;
            elements.importPreviewContainer.style.display = 'block';
        } catch (err) {
            console.error('Import parse error:', err);
            alert('Error parsing file: ' + err.message);
        }
    };
    reader.readAsText(file);
}

function parseCSV(csvText) {
    const lines = csvText.split('\\n');
    if (lines.length < 2) return [];
    const workouts = [];
    // Assume header: Date,Type,Distance,Title,Description
    for (let i = 1; i < lines.length; i++) {
        const line = lines[i].trim();
        if (!line) continue;
        const parts = line.split(',');
        if (parts.length >= 3) {
            workouts.push({
                date: parts[0].trim(),
                type: parts[1].trim(),
                distance: parseFloat(parts[2].trim()) || 0,
                title: parts.length > 3 ? parts[3].trim() : parts[1].trim(),
                description: parts.length > 4 ? parts[4].trim() : ''
            });
        }
    }
    return workouts;
}

function renderImportPreview(workouts) {
    const tbody = elements.importPreviewTable.querySelector('tbody');
    tbody.innerHTML = '';
    // Show up to 10 rows for preview
    workouts.slice(0, 10).forEach(w => {
        const tr = document.createElement('tr');
        tr.innerHTML = `<td style='padding:5px;'>${w.date}</td><td style='padding:5px;'>${w.type}</td><td style='padding:5px;'>${w.distance}</td>`;
        tbody.appendChild(tr);
    });
    if (workouts.length > 10) {
        const tr = document.createElement('tr');
        tr.innerHTML = `<td colspan='3' style='padding:5px; text-align:center; color:#aaa;'>... and ${workouts.length - 10} more</td>`;
        tbody.appendChild(tr);
    }
}

async function processImportedPlan() {
    if (!window.pendingImportPlan || window.pendingImportPlan.length === 0) return;
    
    elements.btnWizImportFinish.disabled = true;
    elements.btnWizImportFinish.innerText = 'SAVING...';
    
    const action = elements.wizImportAction.value; // 'merge' or 'replace'
    
    try {
        if (action === 'replace') {
            // Wipe existing workouts node for this user
            await window.firebaseDb.ref('workouts/' + appState.runnerId).remove();
        }
        
        const updates = {};
        window.pendingImportPlan.forEach(w => {
            const workoutId = window.firebaseDb.ref('workouts/' + appState.runnerId).push().key;
            updates[workoutId] = {
                date: w.date,
                type: w.type,
                distanceKm: w.distance || 0,
                title: w.title || w.type,
                description: w.description || '',
                completed: false,
                durationMinutes: 0
            };
        });
        
        await window.firebaseDb.ref('workouts/' + appState.runnerId).update(updates);
        
        alert('Plan imported successfully!');
        closeWizardModal();
        // Data listeners will automatically refresh UI
    } catch (err) {
        console.error('Save error:', err);
        alert('Failed to save imported plan.');
    } finally {
        elements.btnWizImportFinish.disabled = false;
        elements.btnWizImportFinish.innerText = 'IMPORT PLAN';
    }
}
"""

with open(r'c:\Users\Bruker\Android Studio\Maratontrener\web\app.js', 'a', encoding='utf-8') as f:
    f.write(code)
print("Appended import logic to app.js")
