package com.jostein.maratontrener;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

public class CreatePlanActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        finish();
    }
    /*


    private View page1Goal, page2Athlete, page3Schedule;
    private TextView textStepTitle;
    private Button continueButton;
    private int currentPage = 1;

    // Page 1: Goal
    private EditText eventNameInput, eventLocationInput, dateInput, goalTimeInput;
    private Spinner raceTypeSpinner;
    private Calendar selectedRaceDate;

    // Page 2: Athlete
    private EditText inputAge, inputWeight, inputMaxHR, inputPB10k, inputPBHalf, inputPBFull;

    // Page 3: Schedule
    private EditText startDateInput;
    private Spinner daysPerWeekSpinner;
    private Calendar selectedStartDate;
    private CheckBox checkMonday, checkTuesday, checkWednesday, checkThursday,
            checkFriday, checkSaturday, checkSunday, checkIncludeStrength;
    private Spinner spinnerMonday, spinnerTuesday, spinnerWednesday, spinnerThursday,
            spinnerFriday, spinnerSaturday, spinnerSunday;

    private static final String[] WORKOUT_TYPES = {"STEADY RUN", "INTERVALS", "LONG RUN", "TEMPO RUN", "STRENGTH & CORE"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);

        // UI Components
        page1Goal = findViewById(R.id.page1Goal);
        page2Athlete = findViewById(R.id.page2Athlete);
        page3Schedule = findViewById(R.id.page3Schedule);
        textStepTitle = findViewById(R.id.textStepTitle);
        continueButton = findViewById(R.id.continueButton);

        // Page 1 Init
        eventNameInput = findViewById(R.id.eventNameInput);
        eventLocationInput = findViewById(R.id.eventLocationInput);
        dateInput = findViewById(R.id.dateInput);
        goalTimeInput = findViewById(R.id.goalTimeInput);
        raceTypeSpinner = findViewById(R.id.raceTypeSpinner);
        selectedRaceDate = Calendar.getInstance();
        setupGoalTimeAutoFormat();

        // Page 2 Init
        inputAge = findViewById(R.id.inputAge);
        inputWeight = findViewById(R.id.inputWeight);
        inputMaxHR = findViewById(R.id.inputMaxHR);
        inputPB10k = findViewById(R.id.inputPB10k);
        inputPBHalf = findViewById(R.id.inputPBHalf);
        inputPBFull = findViewById(R.id.inputPBFull);
        setupPBAutoFormat(inputPB10k);
        setupPBAutoFormat(inputPBHalf);
        setupPBAutoFormat(inputPBFull);
        SecurityUtils.setupCommaToDotWatcher(inputWeight);

        // Page 3 Init
        startDateInput = findViewById(R.id.startDateInput);
        daysPerWeekSpinner = findViewById(R.id.daysPerWeekSpinner);
        checkIncludeStrength = findViewById(R.id.checkIncludeStrength);
        checkMonday = findViewById(R.id.checkMonday);
        checkTuesday = findViewById(R.id.checkTuesday);
        checkWednesday = findViewById(R.id.checkWednesday);
        checkThursday = findViewById(R.id.checkThursday);
        checkFriday = findViewById(R.id.checkFriday);
        checkSaturday = findViewById(R.id.checkSaturday);
        checkSunday = findViewById(R.id.checkSunday);
        spinnerMonday = findViewById(R.id.spinnerMonday);
        spinnerTuesday = findViewById(R.id.spinnerTuesday);
        spinnerWednesday = findViewById(R.id.spinnerWednesday);
        spinnerThursday = findViewById(R.id.spinnerThursday);
        spinnerFriday = findViewById(R.id.spinnerFriday);
        spinnerSaturday = findViewById(R.id.spinnerSaturday);
        spinnerSunday = findViewById(R.id.spinnerSunday);
        selectedStartDate = Calendar.getInstance();

        setupDaySpinners();
        setupSpinners();
        setupDatePickers();

        continueButton.setOnClickListener(v -> navigateNext());
        findViewById(R.id.btnCancelPlan).setOnClickListener(v -> checkUnsavedChanges());

        loadExistingData();
        updateStepUI();
    }

    private void checkUnsavedChanges() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Exit Setup?")
                .setMessage("Your plan setup progress will be lost. Do you want to exit?")
                .setPositiveButton("Yes", (dialog, which) -> finish())
                .setNegativeButton("No", null)
                .show();
    }

    @Override
    public void onBackPressed() {
        if (currentPage > 1) {
            currentPage--;
            updateStepUI();
        } else {
            checkUnsavedChanges();
        }
    }

    private void navigateNext() {
        if (currentPage == 1) {
            if (eventNameInput.getText().toString().trim().isEmpty() || dateInput.getText().toString().isEmpty()) {
                Toast.makeText(this, "Please enter race details", Toast.LENGTH_SHORT).show();
                return;
            }
            currentPage = 2;
        } else if (currentPage == 2) {
            currentPage = 3;
        } else {
            generatePlan();
            return;
        }
        updateStepUI();
    }

    private void updateStepUI() {
        page1Goal.setVisibility(currentPage == 1 ? View.VISIBLE : View.GONE);
        page2Athlete.setVisibility(currentPage == 2 ? View.VISIBLE : View.GONE);
        page3Schedule.setVisibility(currentPage == 3 ? View.VISIBLE : View.GONE);

        textStepTitle.setText("STEP " + currentPage + " OF 3");
        continueButton.setText(currentPage == 3 ? "GENERATE MY PLAN" : "NEXT STEP");
    }

    private void generatePlan() {
        String eventName = eventNameInput.getText().toString().trim();
        String selectedRaceType = raceTypeSpinner.getSelectedItem().toString();
        String daysStr = daysPerWeekSpinner.getSelectedItem().toString();

        Set<String> preferredDays = new HashSet<>();
        if (checkMonday.isChecked()) preferredDays.add("Monday");
        if (checkTuesday.isChecked()) preferredDays.add("Tuesday");
        if (checkWednesday.isChecked()) preferredDays.add("Wednesday");
        if (checkThursday.isChecked()) preferredDays.add("Thursday");
        if (checkFriday.isChecked()) preferredDays.add("Friday");
        if (checkSaturday.isChecked()) preferredDays.add("Saturday");
        if (checkSunday.isChecked()) preferredDays.add("Sunday");

        if (preferredDays.isEmpty()) {
            Toast.makeText(this, "Select at least one training day", Toast.LENGTH_SHORT).show();
            return;
        }

        // Save All Prefs (Race + Profile)
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        prefs.edit()
                .putString("eventName", eventName)
                .putString("eventLocation", eventLocationInput.getText().toString().trim())
                .putLong("eventDate", selectedRaceDate.getTimeInMillis())
                .putString("eventType", selectedRaceType)
                .putString("targetTime", goalTimeInput.getText().toString())
                .putString("userAge", inputAge.getText().toString())
                .putString("userWeight", inputWeight.getText().toString())
                .putString("userMaxHR", inputMaxHR.getText().toString())
                .putString("pb10k", inputPB10k.getText().toString())
                .putString("pbHalf", inputPBHalf.getText().toString())
                .putString("pbFull", inputPBFull.getText().toString())
                .putInt("daysPerWeek", Integer.parseInt(daysStr))
                .putStringSet("preferredDays", preferredDays)
                .putBoolean("includeStrength", checkIncludeStrength.isChecked())
                .apply();

        // Actual plan generation
        Bundle workoutAssignments = new Bundle();
        if (checkMonday.isChecked()) workoutAssignments.putString("Monday", spinnerMonday.getSelectedItem().toString());
        if (checkTuesday.isChecked()) workoutAssignments.putString("Tuesday", spinnerTuesday.getSelectedItem().toString());
        if (checkWednesday.isChecked()) workoutAssignments.putString("Wednesday", spinnerWednesday.getSelectedItem().toString());
        if (checkThursday.isChecked()) workoutAssignments.putString("Thursday", spinnerThursday.getSelectedItem().toString());
        if (checkFriday.isChecked()) workoutAssignments.putString("Friday", spinnerFriday.getSelectedItem().toString());
        if (checkSaturday.isChecked()) workoutAssignments.putString("Saturday", spinnerSaturday.getSelectedItem().toString());
        if (checkSunday.isChecked()) workoutAssignments.putString("Sunday", spinnerSunday.getSelectedItem().toString());

        generateAndSavePlanToDB(selectedRaceDate, selectedStartDate, selectedRaceType, preferredDays, workoutAssignments, eventName, checkIncludeStrength.isChecked());
    }

    private void generateAndSavePlanToDB(Calendar raceDate, Calendar startTrainingDate, String raceType, Set<String> days, Bundle assignments, String eventName, boolean includeStrength) {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                WorkoutDatabase db = WorkoutDatabase.getDatabase(getApplicationContext());
                SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
                prefs.edit()
                    .putLong("planStartDate", startTrainingDate.getTimeInMillis())
                    .putLong("profileLastUpdate", System.currentTimeMillis())
                    .apply();
                
                String targetTimeStr = prefs.getString("targetTime", raceType.contains("Half") ? "02:00:00" : "04:00:00");
                double targetHours = parseTime(targetTimeStr); 
                double raceDistance = raceType.contains("Half") ? 21.1 : (raceType.contains("10") ? 10.0 : 42.2);
                double racePaceMinPerKm = (targetHours * 60.0) / raceDistance;
                
                db.workoutDao().deleteUncompletedWorkouts();

                List<WorkoutEntity> plan = new ArrayList<>();
                Calendar current = (Calendar) startTrainingDate.clone();
                current.set(Calendar.HOUR_OF_DAY, 0); current.set(Calendar.MINUTE, 0);

                long totalDays = (raceDate.getTimeInMillis() - current.getTimeInMillis()) / (1000 * 60 * 60 * 24);
                int totalWeeks = (int) (totalDays / 7);
                if (totalWeeks < 1) totalWeeks = 1;
                int currentWeek = 0;

                String strengthDay = includeStrength ? "Friday" : "";
                String[] runningTypes = {"INTERVALS", "STEADY RUN", "LONG RUN"};
                int runCounter = 0;

                while (current.before(raceDate)) {
                    int dayOfWeek = current.get(Calendar.DAY_OF_WEEK);
                    String dayName = getDayName(dayOfWeek);

                    if (days.contains(dayName)) {
                        WorkoutEntity workout = new WorkoutEntity();
                        workout.setScheduledDate(current.getTimeInMillis());
                        workout.setPlanName(eventName);
                        workout.setWeekNumber(currentWeek + 1);

                        String type;
                        if (includeStrength && dayName.equals(strengthDay)) {
                            type = "STRENGTH & CORE";
                        } else {
                            type = assignments.getString(dayName, runningTypes[runCounter % runningTypes.length]);
                            if (!"STRENGTH & CORE".equalsIgnoreCase(type)) runCounter++;
                        }
                        workout.setWorkoutType(type);

                        // Phase Math
                        double progress = (double) currentWeek / totalWeeks;
                        String phase = (progress > 0.85) ? "TAPER" : (progress > 0.4 ? "PEAK" : "BASE");

                        if ("LONG RUN".equalsIgnoreCase(type)) {
                            double base, max;
                            if (raceType.contains("5K")) {
                                base = 4.0; max = 7.0;
                            } else if (raceType.contains("10K")) {
                                base = 6.0; max = 12.0;
                            } else if (raceType.contains("Half")) {
                                base = 8.0; max = 18.0;
                            } else {
                                base = 12.0; max = 30.0;
                            }
                            double p = racePaceMinPerKm * 1.2;
                            workout.setDistance(phase.equals("TAPER") ? (max * 0.7) : Math.min(base + (currentWeek * 1.5), max));
                            workout.setDescription(getString(R.string.desc_endurance, formatPace(p)));
                        } else if ("INTERVALS".equalsIgnoreCase(type)) {
                            int reps = 4 + (currentWeek / 3);
                            double p = racePaceMinPerKm * 0.95;
                            workout.setIntervalCount(reps);
                            workout.setIntervalValue("800m");
                            workout.setIntervalPace(formatPace(p));
                            workout.setDescription(getString(R.string.desc_speedwork, reps, formatPace(p)));
                        } else if ("STRENGTH & CORE".equalsIgnoreCase(type)) {
                            int wk = currentWeek + 1;
                            if (wk <= 4) {
                                workout.setDescription(getString(R.string.desc_strength_phase1));
                            } else if (wk <= 8) {
                                workout.setDescription(getString(R.string.desc_strength_phase2));
                            } else {
                                workout.setDescription(getString(R.string.desc_strength_phase3));
                            }
                        } else if ("TEMPO RUN".equalsIgnoreCase(type)) {
                            double base, prog;
                            if (raceType.contains("5K")) {
                                base = 3.0; prog = 0.2;
                            } else if (raceType.contains("10K")) {
                                base = 4.0; prog = 0.3;
                            } else if (raceType.contains("Half")) {
                                base = 5.0; prog = 0.5;
                            } else {
                                base = 6.0; prog = 0.5;
                            }
                            double p = racePaceMinPerKm * 1.05;
                            workout.setDistance(base + (currentWeek * prog));
                            workout.setDescription(getString(R.string.desc_threshold, formatPace(p)));
                        } else {
                            double base, prog;
                            if (raceType.contains("5K")) {
                                base = 3.0; prog = 0.2;
                            } else if (raceType.contains("10K")) {
                                base = 4.0; prog = 0.3;
                            } else if (raceType.contains("Half")) {
                                base = 5.0; prog = 0.5;
                            } else {
                                base = 6.0; prog = 0.5;
                            }
                            double p = racePaceMinPerKm * 1.12;
                            workout.setDistance(base + (currentWeek * prog));
                            workout.setDescription(getString(R.string.desc_easy, formatPace(p)));
                        }
                        plan.add(workout);
                    }
                    if (dayOfWeek == Calendar.SUNDAY) currentWeek++;
                    current.add(Calendar.DATE, 1);
                }

                db.workoutDao().insertAll(plan);
                WorkoutUtils.uploadWorkoutsToFirebase(getApplicationContext());
                runOnUiThread(() -> {
                    Intent intent = new Intent(CreatePlanActivity.this, MainContainerActivity.class);
                    intent.putExtra("SELECT_TAB", R.id.nav_profile);
                    startActivity(intent);
                    finish();
                });
            } catch (Exception e) {
                Log.e("CreatePlan", "Failed", e);
            }
        });
    }

    private double parseTime(String time) {
        try {
            String[] p = time.split(":");
            if (p.length == 3) return Integer.parseInt(p[0]) + (Integer.parseInt(p[1])/60.0) + (Integer.parseInt(p[2])/3600.0);
            if (p.length == 2) {
                int first = Integer.parseInt(p[0]);
                if (first >= 12) {
                    // Treat as MM:SS (minutes, convert to hours)
                    return (first / 60.0) + (Integer.parseInt(p[1]) / 3600.0);
                } else {
                    // Treat as HH:MM
                    return first + (Integer.parseInt(p[1]) / 60.0);
                }
            }
            return Double.parseDouble(time);
        } catch (Exception e) { return 4.0; }
    }

    private String formatPace(double decimalPace) {
        int m = (int) decimalPace;
        int s = (int) Math.round((decimalPace - m) * 60);
        return String.format(Locale.getDefault(), "%d:%02d", m, s);
    }

    private void setupSpinners() {
        ArrayAdapter<CharSequence> rAdapter = ArrayAdapter.createFromResource(this, R.array.event_types, R.layout.spinner_item);
        rAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        raceTypeSpinner.setAdapter(rAdapter);

        ArrayAdapter<CharSequence> dAdapter = ArrayAdapter.createFromResource(this, R.array.training_days_options, R.layout.spinner_item);
        dAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        daysPerWeekSpinner.setAdapter(dAdapter);
        daysPerWeekSpinner.setSelection(2); // 3 days default
    }

    private void setupDatePickers() {
        dateInput.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, y, m, d) -> {
                selectedRaceDate.set(y, m, d);
                dateInput.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(selectedRaceDate.getTime()));
            }, selectedRaceDate.get(Calendar.YEAR), selectedRaceDate.get(Calendar.MONTH), selectedRaceDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        startDateInput.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, y, m, d) -> {
                selectedStartDate.set(y, m, d);
                startDateInput.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(selectedStartDate.getTime()));
            }, selectedStartDate.get(Calendar.YEAR), selectedStartDate.get(Calendar.MONTH), selectedStartDate.get(Calendar.DAY_OF_MONTH)).show();
        });
        startDateInput.setText(new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(selectedStartDate.getTime()));
    }

    private void setupDaySpinners() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_item, WORKOUT_TYPES);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerMonday.setAdapter(adapter); spinnerTuesday.setAdapter(adapter);
        spinnerWednesday.setAdapter(adapter); spinnerThursday.setAdapter(adapter);
        spinnerFriday.setAdapter(adapter); spinnerSaturday.setAdapter(adapter);
        spinnerSunday.setAdapter(adapter);
        
        spinnerMonday.setSelection(1); spinnerTuesday.setSelection(0);
        spinnerWednesday.setSelection(0); spinnerThursday.setSelection(0);
        spinnerFriday.setSelection(0); spinnerSaturday.setSelection(0);
        spinnerSunday.setSelection(2);
    }

    private void setupGoalTimeAutoFormat() {
        goalTimeInput.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                String input = s.toString().replaceAll("[^\\d]", "");
                if (input.length() > 6) input = input.substring(0, 6);
                StringBuilder formatted = new StringBuilder();
                if (input.length() > 0) {
                    if (input.length() <= 2) formatted.append(input);
                    else if (input.length() <= 4) formatted.append(input.substring(0, input.length()-2)).append(":").append(input.substring(input.length()-2));
                    else formatted.append(input.substring(0, input.length()-4)).append(":").append(input.substring(input.length()-4, input.length()-2)).append(":").append(input.substring(input.length()-2));
                }
                isUpdating = true;
                goalTimeInput.setText(formatted.toString());
                goalTimeInput.setSelection(formatted.length());
                isUpdating = false;
            }
        });
    }

    private void setupPBAutoFormat(EditText et) {
        et.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                String input = s.toString().replaceAll("[^\\d]", "");
                if (input.length() > 6) input = input.substring(0, 6);
                StringBuilder f = new StringBuilder();
                if (input.length() > 0) {
                    if (input.length() <= 2) f.append(input);
                    else if (input.length() <= 4) f.append(input.substring(0, input.length()-2)).append(":").append(input.substring(input.length()-2));
                    else f.append(input.substring(0, input.length()-4)).append(":").append(input.substring(input.length()-4, input.length()-2)).append(":").append(input.substring(input.length()-2));
                }
                isUpdating = true;
                et.setText(f.toString());
                et.setSelection(f.length());
                isUpdating = false;
            }
        });
    }

    private void loadExistingData() {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        eventNameInput.setText(prefs.getString("eventName", "Oslo Maraton"));
        eventLocationInput.setText(prefs.getString("eventLocation", ""));
        goalTimeInput.setText(prefs.getString("targetTime", "04:00:00"));
        inputAge.setText(prefs.getString("userAge", ""));
        inputWeight.setText(prefs.getString("userWeight", ""));
        inputMaxHR.setText(prefs.getString("userMaxHR", ""));
        inputPB10k.setText(prefs.getString("pb10k", ""));
        inputPBHalf.setText(prefs.getString("pbHalf", ""));
        inputPBFull.setText(prefs.getString("pbFull", ""));
    }

    private String getDayName(int dayOfWeek) {
        switch (dayOfWeek) {
            case Calendar.MONDAY: return "Monday";
            case Calendar.TUESDAY: return "Tuesday";
            case Calendar.WEDNESDAY: return "Wednesday";
            case Calendar.THURSDAY: return "Thursday";
            case Calendar.FRIDAY: return "Friday";
            case Calendar.SATURDAY: return "Saturday";
            case Calendar.SUNDAY: return "Sunday";
            default: return "";
        }
    }
    */
}
