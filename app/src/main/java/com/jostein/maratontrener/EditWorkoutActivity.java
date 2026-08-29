package com.jostein.maratontrener;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;
import com.jostein.maratontrener.database.ShoeDao;
import com.jostein.maratontrener.database.ShoeEntity;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executors;

public class EditWorkoutActivity extends AppCompatActivity {

    private WorkoutDao workoutDao;
    private WorkoutEntity workout;
    private EditText editDistance, editPace, editDuration, editAvgHR, editMaxHR, editDescription, editNotes, editDate;
    private EditText editIntervalCount, editIntervalValue, editIntervalPace;
    private View containerStandardFields, containerIntervalFields;
    private View btnDeleteWorkout;
    private CheckBox completedCheckbox;
    private Spinner spinnerWorkoutType;
    private Spinner spinnerWorkoutShoe;
    private View cardStrengthGuides;
    private List<ShoeEntity> activeShoesList = new ArrayList<>();
    private long selectedDateMillis;
    private boolean isInternalUpdate = false;
    private boolean hasUnsavedChanges = false;
    private long activityStartTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_workout);
        activityStartTime = System.currentTimeMillis();

        editDistance = findViewById(R.id.editDistance);
        editPace = findViewById(R.id.editPace);
        editDuration = findViewById(R.id.editDuration);
        editAvgHR = findViewById(R.id.editAvgHR);
        editMaxHR = findViewById(R.id.editMaxHR);
        editIntervalCount = findViewById(R.id.editIntervalCount);
        editIntervalValue = findViewById(R.id.editIntervalValue);
        editIntervalPace = findViewById(R.id.editIntervalPace);
        containerStandardFields = findViewById(R.id.containerStandardFields);
        containerIntervalFields = findViewById(R.id.containerIntervalFields);
        editDescription = findViewById(R.id.editDescription);
        editNotes = findViewById(R.id.editNotes);
        editDate = findViewById(R.id.editDate);
        completedCheckbox = findViewById(R.id.completedCheckbox);
        spinnerWorkoutType = findViewById(R.id.spinnerWorkoutType);
        spinnerWorkoutShoe = findViewById(R.id.spinnerWorkoutShoe);
        btnDeleteWorkout = findViewById(R.id.btnDeleteWorkout);
        cardStrengthGuides = findViewById(R.id.cardStrengthGuides);
        Button saveButton = findViewById(R.id.saveButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        // Bind clicks for exercise guide items
        findViewById(R.id.btnGuideGluteBridges).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=glute+bridge+form+for+runners"));
            startActivity(intent);
        });

        findViewById(R.id.btnGuidePlanks).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=plank+form+for+runners"));
            startActivity(intent);
        });

        findViewById(R.id.btnGuideSingleLegSquats).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=single+leg+squat+form+for+runners"));
            startActivity(intent);
        });

        editDate.setOnClickListener(v -> showDatePicker());
        btnDeleteWorkout.setOnClickListener(v -> confirmDelete());

        setupPaceAutoFormat();
        setupDurationAutoFormat();
        setupSmartCalculations();

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.workout_types, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerWorkoutType.setAdapter(adapter);

        spinnerWorkoutType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if ("INTERVALS".equalsIgnoreCase(selected)) {
                    containerStandardFields.setVisibility(View.GONE);
                    containerIntervalFields.setVisibility(View.VISIBLE);
                    cardStrengthGuides.setVisibility(View.GONE);
                } else if ("STRENGTH & CORE".equalsIgnoreCase(selected)) {
                    containerStandardFields.setVisibility(View.VISIBLE);
                    containerIntervalFields.setVisibility(View.GONE);
                    cardStrengthGuides.setVisibility(View.VISIBLE);
                } else {
                    containerStandardFields.setVisibility(View.VISIBLE);
                    containerIntervalFields.setVisibility(View.GONE);
                    cardStrengthGuides.setVisibility(View.GONE);
                }
                if (!isInternalUpdate && (System.currentTimeMillis() - activityStartTime > 1000)) {
                    hasUnsavedChanges = true;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerWorkoutShoe.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isInternalUpdate && (System.currentTimeMillis() - activityStartTime > 1000)) {
                    hasUnsavedChanges = true;
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        workoutDao = WorkoutDatabase.getDatabase(this).workoutDao();
        loadShoes();
        int workoutId = getIntent().getIntExtra("WORKOUT_ID", -1);

        if (workoutId != -1) {
            btnDeleteWorkout.setVisibility(View.VISIBLE);
            loadWorkout(workoutId);
        } else {
            selectedDateMillis = System.currentTimeMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            editDate.setText(sdf.format(new Date(selectedDateMillis)));
        }

        saveButton.setOnClickListener(v -> saveWorkout());
        cancelButton.setOnClickListener(v -> checkUnsavedChanges());
        findViewById(R.id.btnBackEditWorkout).setOnClickListener(v -> checkUnsavedChanges());
        
        setupChangeTracking();
    }

    private void setupChangeTracking() {
        TextWatcher changeWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isInternalUpdate || (System.currentTimeMillis() - activityStartTime < 1000)) return;
                View focused = getCurrentFocus();
                if (focused != null && focused instanceof EditText) {
                    hasUnsavedChanges = true;
                }
            }
        };

        editDistance.addTextChangedListener(changeWatcher);
        editPace.addTextChangedListener(changeWatcher);
        editDuration.addTextChangedListener(changeWatcher);
        editAvgHR.addTextChangedListener(changeWatcher);
        editMaxHR.addTextChangedListener(changeWatcher);
        editIntervalCount.addTextChangedListener(changeWatcher);
        editIntervalValue.addTextChangedListener(changeWatcher);
        editIntervalPace.addTextChangedListener(changeWatcher);
        editDescription.addTextChangedListener(changeWatcher);
        editNotes.addTextChangedListener(changeWatcher);
        
        completedCheckbox.setOnClickListener(v -> hasUnsavedChanges = true);
    }

    private void checkUnsavedChanges() {
        if (hasUnsavedChanges) {
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("Your changes have not been saved. Do you want to exit without saving?")
                    .setPositiveButton("Yes", (dialog, which) -> finish())
                    .setNegativeButton("No", null)
                    .show();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        checkUnsavedChanges();
    }

    private void loadWorkout(int id) {
        Executors.newSingleThreadExecutor().execute(() -> {
            workout = workoutDao.getWorkoutById(id);
            if (workout != null) {
                runOnUiThread(() -> {
                    isInternalUpdate = true;
                    editDistance.setText(String.valueOf(workout.getDistance()));
                    double pace = workout.getPace();
                    if (pace > 0) {
                        int min = (int) pace;
                        int sec = (int) Math.round((pace - min) * 60);
                        editPace.setText(String.format(Locale.getDefault(), "%02d:%02d", min, sec));
                    } else { editPace.setText(""); }
                    double duration = workout.getTotalDuration();
                    if (duration > 0) {
                        int h = (int) (duration / 60); int m = (int) (duration % 60); int s = (int) Math.round((duration * 60) % 60);
                        editDuration.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s));
                    } else { editDuration.setText(""); }
                    editAvgHR.setText(workout.getAvgHeartRate() > 0 ? String.valueOf(workout.getAvgHeartRate()) : "");
                    editMaxHR.setText(workout.getMaxHeartRate() > 0 ? String.valueOf(workout.getMaxHeartRate()) : "");
                    editIntervalCount.setText(workout.getIntervalCount() > 0 ? String.valueOf(workout.getIntervalCount()) : "");
                    editIntervalValue.setText(workout.getIntervalValue() != null ? workout.getIntervalValue() : "");
                    editIntervalPace.setText(workout.getIntervalPace() != null ? workout.getIntervalPace() : "");
                    editDescription.setText(workout.getDescription());
                    editNotes.setText(workout.getNotes());
                    completedCheckbox.setChecked(workout.isCompleted());
                    selectedDateMillis = workout.getScheduledDate();
                    SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
                    editDate.setText(sdf.format(new Date(selectedDateMillis)));
                    if (workout.getWorkoutType() != null) {
                        ArrayAdapter adapter = (ArrayAdapter) spinnerWorkoutType.getAdapter();
                        int pos = adapter.getPosition(workout.getWorkoutType());
                        spinnerWorkoutType.setSelection(pos);
                        if ("INTERVALS".equalsIgnoreCase(workout.getWorkoutType())) {
                            containerStandardFields.setVisibility(View.GONE);
                            containerIntervalFields.setVisibility(View.VISIBLE);
                            cardStrengthGuides.setVisibility(View.GONE);
                        } else if ("STRENGTH & CORE".equalsIgnoreCase(workout.getWorkoutType())) {
                            containerStandardFields.setVisibility(View.VISIBLE);
                            containerIntervalFields.setVisibility(View.GONE);
                            cardStrengthGuides.setVisibility(View.VISIBLE);
                        } else {
                            containerStandardFields.setVisibility(View.VISIBLE);
                            containerIntervalFields.setVisibility(View.GONE);
                            cardStrengthGuides.setVisibility(View.GONE);
                        }
                    }
                    setShoeSelection();
                    hasUnsavedChanges = false;
                    isInternalUpdate = false;
                });
            }
        });
    }

    private void setupDurationAutoFormat() {
        editDuration.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isInternalUpdate) return;
                String input = s.toString().replaceAll("[^\\d]", "");
                if (input.length() > 6) input = input.substring(0, 6);
                StringBuilder formatted = new StringBuilder();
                if (!input.isEmpty()) {
                    if (input.length() <= 2) formatted.append(input);
                    else if (input.length() <= 4) formatted.append(input.substring(0, input.length() - 2)).append(":").append(input.substring(input.length() - 2));
                    else formatted.append(input.substring(0, input.length() - 4)).append(":").append(input.substring(input.length() - 4, input.length() - 2)).append(":").append(input.substring(input.length() - 2));
                }
                String finalFormatted = formatted.toString();
                if (!finalFormatted.equals(s.toString())) {
                    isInternalUpdate = true; editDuration.setText(finalFormatted); editDuration.setSelection(finalFormatted.length()); isInternalUpdate = false;
                }
            }
        });
    }

    private void setupSmartCalculations() {
        TextWatcher calcWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isInternalUpdate && (editDistance.isFocused() || editPace.isFocused() || editDuration.isFocused())) calculateMissingField();
            }
        };
        editDistance.addTextChangedListener(calcWatcher); editPace.addTextChangedListener(calcWatcher); editDuration.addTextChangedListener(calcWatcher);
    }

    private void calculateMissingField() {
        try {
            String distStr = editDistance.getText().toString(); String paceStr = editPace.getText().toString(); String durStr = editDuration.getText().toString();
            double dist = distStr.isEmpty() ? 0 : Double.parseDouble(distStr.replace(",", "."));
            double pace = 0; if (paceStr.contains(":")) { String[] p = paceStr.split(":"); pace = Double.parseDouble(p[0]) + (Double.parseDouble(p[1]) / 60.0); }
            double dur = 0; if (durStr.contains(":")) { String[] p = durStr.split(":"); if (p.length == 3) dur = Double.parseDouble(p[0]) * 60 + Double.parseDouble(p[1]) + (Double.parseDouble(p[2]) / 60.0); else if (p.length == 2) dur = Double.parseDouble(p[0]) + (Double.parseDouble(p[1]) / 60.0); }
            if (editDistance.isFocused() && pace > 0) updateDurationField(dist * pace);
            else if (editPace.isFocused() && dist > 0) updateDurationField(dist * pace);
            else if (editDuration.isFocused() && dist > 0 && dur > 0) updatePaceField(dur / dist);
        } catch (Exception ignored) {}
    }

    private void updateDurationField(double minutes) {
        if (minutes < 0 || Double.isInfinite(minutes) || Double.isNaN(minutes)) return;
        int h = (int) (minutes / 60); int m = (int) (minutes % 60); int s = (int) Math.round((minutes * 60) % 60);
        String formatted = String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
        if (!formatted.equals(editDuration.getText().toString())) { isInternalUpdate = true; editDuration.setText(formatted); isInternalUpdate = false; }
    }

    private void updatePaceField(double decimalPace) {
        if (decimalPace < 0 || Double.isInfinite(decimalPace) || Double.isNaN(decimalPace)) return;
        int m = (int) decimalPace; int s = (int) Math.round((decimalPace - m) * 60);
        String formatted = String.format(Locale.getDefault(), "%02d:%02d", m, s);
        if (!formatted.equals(editPace.getText().toString())) { isInternalUpdate = true; editPace.setText(formatted); isInternalUpdate = false; }
    }

    private void setupPaceAutoFormat() {
        editPace.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isInternalUpdate) return;
                String input = s.toString().replaceAll("[^\\d]", "");
                if (input.length() > 4) input = input.substring(0, 4);
                StringBuilder formatted = new StringBuilder();
                if (!input.isEmpty()) { if (input.length() <= 2) formatted.append(input); else { formatted.append(input.substring(0, input.length() - 2)).append(":").append(input.substring(input.length() - 2)); } }
                String finalFormatted = formatted.toString();
                if (!finalFormatted.equals(s.toString())) { isInternalUpdate = true; editPace.setText(finalFormatted); editPace.setSelection(finalFormatted.length()); isInternalUpdate = false; }
            }
        });
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDateMillis > 0) calendar.setTimeInMillis(selectedDateMillis);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            selectedDateMillis = calendar.getTimeInMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            editDate.setText(sdf.format(calendar.getTime()));
            hasUnsavedChanges = true;
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void confirmDelete() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Delete Workout")
                .setMessage("Are you sure you want to delete this workout?")
                .setPositiveButton("Delete", (dialog, which) -> deleteWorkout())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteWorkout() {
        if (workout == null) return;
        Executors.newSingleThreadExecutor().execute(() -> {
            workoutDao.deleteWorkout(workout);
            WorkoutUtils.uploadWorkoutsToFirebase(getApplicationContext());
            runOnUiThread(() -> {
                Toast.makeText(this, "Workout Deleted", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }

    private void saveWorkout() {
        boolean isNew = (workout == null); if (isNew) workout = new WorkoutEntity();
        try {
            workout.setDistance(editDistance.getText().toString().isEmpty() ? 0 : Double.parseDouble(editDistance.getText().toString().replace(",", ".")));
            String paceStr = editPace.getText().toString();
            if (paceStr.contains(":")) { String[] parts = paceStr.split(":"); workout.setPace(Double.parseDouble(parts[0]) + (Double.parseDouble(parts[1]) / 60.0)); } else { workout.setPace(0); }
            String durStr = editDuration.getText().toString();
            if (durStr.contains(":")) { String[] parts = durStr.split(":"); if (parts.length == 3) workout.setTotalDuration(Double.parseDouble(parts[0]) * 60 + Double.parseDouble(parts[1]) + (Double.parseDouble(parts[2]) / 60.0)); else if (parts.length == 2) workout.setTotalDuration(Double.parseDouble(parts[0]) + (Double.parseDouble(parts[1]) / 60.0)); } else { workout.setTotalDuration(0); }
            workout.setAvgHeartRate(editAvgHR.getText().toString().isEmpty() ? 0 : Integer.parseInt(editAvgHR.getText().toString()));
            workout.setMaxHeartRate(editMaxHR.getText().toString().isEmpty() ? 0 : Integer.parseInt(editMaxHR.getText().toString()));
            if ("INTERVALS".equalsIgnoreCase(spinnerWorkoutType.getSelectedItem().toString())) {
                workout.setIntervalCount(editIntervalCount.getText().toString().isEmpty() ? 0 : Integer.parseInt(editIntervalCount.getText().toString()));
                workout.setIntervalValue(editIntervalValue.getText().toString()); workout.setIntervalPace(editIntervalPace.getText().toString());
                workout.setDistance(0); workout.setPace(0); workout.setTotalDuration(0);
            } else { workout.setIntervalCount(0); workout.setIntervalValue(null); workout.setIntervalPace(null); }
            workout.setDescription(editDescription.getText().toString()); workout.setNotes(editNotes.getText().toString()); workout.setCompleted(completedCheckbox.isChecked()); workout.setWorkoutType(spinnerWorkoutType.getSelectedItem().toString()); workout.setScheduledDate(selectedDateMillis);

            int shoeSelection = spinnerWorkoutShoe.getSelectedItemPosition();
            if (shoeSelection > 0 && shoeSelection - 1 < activeShoesList.size()) {
                workout.setShoeId(activeShoesList.get(shoeSelection - 1).getId());
            } else {
                workout.setShoeId(null);
            }

            Executors.newSingleThreadExecutor().execute(() -> {
                if (isNew) workoutDao.insertWorkout(workout); else workoutDao.updateWorkout(workout);
                WorkoutUtils.uploadWorkoutsToFirebase(getApplicationContext());
                runOnUiThread(() -> { Toast.makeText(this, "Workout Saved", Toast.LENGTH_SHORT).show(); hasUnsavedChanges = false; finish(); });
            });
        } catch (NumberFormatException e) { Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show(); }
    }

    private void loadShoes() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                ShoeDao shoeDao = WorkoutDatabase.getDatabase(this).shoeDao();
                List<ShoeEntity> activeShoes = shoeDao.getActiveShoesSync();

                if (workout != null && workout.getShoeId() != null && !workout.getShoeId().isEmpty()) {
                    boolean found = false;
                    for (ShoeEntity s : activeShoes) {
                        if (s.getId().equals(workout.getShoeId())) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        ShoeEntity currentShoe = shoeDao.getShoeByIdSync(workout.getShoeId());
                        if (currentShoe != null) {
                            activeShoes.add(currentShoe);
                        }
                    }
                }

                activeShoesList.clear();
                activeShoesList.addAll(activeShoes);

                List<String> shoeNames = new ArrayList<>();
                shoeNames.add("-- No Shoe Selected --");
                for (ShoeEntity s : activeShoesList) {
                    shoeNames.add(s.getName());
                }

                runOnUiThread(() -> {
                    ArrayAdapter<String> shoeAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, shoeNames);
                    shoeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
                    spinnerWorkoutShoe.setAdapter(shoeAdapter);

                    if (workout != null) {
                        setShoeSelection();
                    }
                });
            } catch (Exception e) {
                android.util.Log.e("EditWorkout", "Error loading shoes: " + e.getMessage());
            }
        });
    }

    private void setShoeSelection() {
        if (spinnerWorkoutShoe == null || spinnerWorkoutShoe.getAdapter() == null) return;
        if (workout == null || workout.getShoeId() == null || workout.getShoeId().isEmpty()) {
            spinnerWorkoutShoe.setSelection(0);
            return;
        }
        for (int i = 0; i < activeShoesList.size(); i++) {
            if (activeShoesList.get(i).getId().equals(workout.getShoeId())) {
                spinnerWorkoutShoe.setSelection(i + 1);
                return;
            }
        }
        spinnerWorkoutShoe.setSelection(0);
    }
}
