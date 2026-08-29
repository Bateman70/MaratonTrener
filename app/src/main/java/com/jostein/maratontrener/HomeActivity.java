package com.jostein.maratontrener;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.imageview.ShapeableImageView;
import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {

    private TextView textRaceName, textHomeRaceCategory, textRaceDate, textHomeRaceLocation, textHomeProgressPercent, textNextType, textNextDetails, textNextDate;
    private ProgressBar progressOverall;
    private View cardNextActivity, cardRaceOverview;
    private TextView textNextSessionLabel;
    private ShapeableImageView imageProfileNav;
    private WorkoutDao workoutDao;
    private BottomNavigationView bottomNavigationView;
    private int latestWorkoutId = -1;
    private int nextWorkoutId = -1;

    // Latest Activity UI
    private View cardLatestActivity, labelLatestActivity;
    private TextView textLatestUserName, textLatestDate, textLatestType, textLatestDistance, textLatestDuration, textLatestPace, textLatestHR;
    private TextView labelLatestDist, labelLatestDur, labelLatestPace;
    private ShapeableImageView imageLatestUser;

    // Pace Calculator UI
    private Spinner spinnerCalcDistance;
    private EditText editCalcCustomDist, editCalcTime, editCalcPace, editCalcSpeed;
    private View inputLayoutCustomDist;
    private boolean isInternalCalcUpdate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        Toolbar toolbar = findViewById(R.id.toolbarHome);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        textRaceName = findViewById(R.id.textHomeRaceName);
        textHomeRaceCategory = findViewById(R.id.textHomeRaceCategory);
        textRaceDate = findViewById(R.id.textHomeRaceDate);
        textHomeRaceLocation = findViewById(R.id.textHomeRaceLocation);
        textHomeProgressPercent = findViewById(R.id.textHomeProgressPercent);
        textNextType = findViewById(R.id.textNextType);
        textNextDetails = findViewById(R.id.textNextDetails);
        textNextDate = findViewById(R.id.textNextDate);
        progressOverall = findViewById(R.id.progressHomeOverall);
        cardNextActivity = findViewById(R.id.cardNextActivity);
        cardRaceOverview = findViewById(R.id.cardRaceOverview);
        textNextSessionLabel = findViewById(R.id.textNextSessionLabel);
        imageProfileNav = findViewById(R.id.imageProfileNav);

        // Latest Activity UI
        cardLatestActivity = findViewById(R.id.cardLatestActivity);
        labelLatestActivity = findViewById(R.id.labelLatestActivity);
        textLatestUserName = findViewById(R.id.textLatestUserName);
        textLatestDate = findViewById(R.id.textLatestDate);
        textLatestType = findViewById(R.id.textLatestType);
        textLatestDistance = findViewById(R.id.textLatestDistance);
        textLatestDuration = findViewById(R.id.textLatestDuration);
        textLatestPace = findViewById(R.id.textLatestPace);
        textLatestHR = findViewById(R.id.textLatestHR);
        labelLatestDist = findViewById(R.id.labelLatestDistance);
        labelLatestDur = findViewById(R.id.labelLatestDuration);
        labelLatestPace = findViewById(R.id.labelLatestPace);
        imageLatestUser = findViewById(R.id.imageLatestUser);

        // Pace Calculator UI
        spinnerCalcDistance = findViewById(R.id.spinnerCalcDistance);
        editCalcCustomDist = findViewById(R.id.editCalcCustomDist);
        editCalcTime = findViewById(R.id.editCalcTime);
        editCalcPace = findViewById(R.id.editCalcPace);
        editCalcSpeed = findViewById(R.id.editCalcSpeed);
        inputLayoutCustomDist = findViewById(R.id.inputLayoutCustomDist);

        setupPaceCalculator();

        workoutDao = WorkoutDatabase.getDatabase(this).workoutDao();

        imageProfileNav.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        findViewById(R.id.btnNavStart).setOnClickListener(v -> {
            Intent intent = new Intent(this, StartActivity.class);
            intent.putExtra("FROM_DASHBOARD", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        cardNextActivity.setOnClickListener(v -> {
            if (nextWorkoutId != -1) {
                Intent intent = new Intent(this, EditWorkoutActivity.class);
                intent.putExtra("WORKOUT_ID", nextWorkoutId);
                startActivity(intent);
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
        });

        cardLatestActivity.setOnClickListener(v -> {
            if (latestWorkoutId != -1) {
                Intent intent = new Intent(this, EditWorkoutActivity.class);
                intent.putExtra("WORKOUT_ID", latestWorkoutId);
                startActivity(intent);
            }
        });

        cardRaceOverview.setOnClickListener(v -> startActivity(new Intent(this, RaceInfoActivity.class)));

        // Ensure bottom navigation is found and setup at the very end
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        if (bottomNavigationView != null) {
            setupNavigation();
        }

        requestNotificationPermission();
        scheduleDailyReminder();
    }

    private void setupNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) return true;
            
            Intent intent = null;
            if (id == R.id.nav_log) intent = new Intent(this, MainActivity.class);
            else if (id == R.id.nav_stats) intent = new Intent(this, ProgressActivity.class);
            else if (id == R.id.nav_buddies) intent = new Intent(this, BuddyActivity.class);
            else if (id == R.id.nav_profile) intent = new Intent(this, ProfileActivity.class);
            
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    private void requestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void scheduleDailyReminder() {
        ReminderReceiver.scheduleDailyReminder(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.getMenu().findItem(R.id.nav_home).setChecked(true);
        }
        loadRaceInfo();
        loadProgressAndNext();
        loadLatestActivity();
        loadProfileImage();
        WorkoutUtils.uploadWorkoutsToFirebase(getApplicationContext());
    }

    private void setupPaceCalculator() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.calc_distances, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerCalcDistance.setAdapter(adapter);

        spinnerCalcDistance.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();
                if ("Custom".equalsIgnoreCase(selected)) {
                    inputLayoutCustomDist.setVisibility(View.VISIBLE);
                } else {
                    inputLayoutCustomDist.setVisibility(View.GONE);
                }
                calculateMissingPaceValue();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        android.text.TextWatcher watcher = new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (!isInternalCalcUpdate) calculateMissingPaceValue();
            }
        };

        editCalcCustomDist.addTextChangedListener(watcher);

        editCalcTime.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (isInternalCalcUpdate) return;
                String input = s.toString().replaceAll("[^\\d]", "");
                if (input.length() > 6) input = input.substring(0, 6);
                StringBuilder formatted = new StringBuilder();
                if (!input.isEmpty()) {
                    if (input.length() <= 2) formatted.append(input);
                    else if (input.length() <= 4) formatted.append(input.substring(0, input.length() - 2)).append(":").append(input.substring(input.length() - 2));
                    else formatted.append(input.substring(0, input.length() - 4)).append(":").append(input.substring(input.length() - 4, input.length() - 2)).append(":").append(input.substring(input.length() - 2));
                }
                isInternalCalcUpdate = true;
                String finalStr = formatted.toString();
                if (!finalStr.equals(s.toString())) {
                    editCalcTime.setText(finalStr);
                    editCalcTime.setSelection(finalStr.length());
                }
                isInternalCalcUpdate = false;
                if (editCalcTime.isFocused()) calculateMissingPaceValue();
            }
        });

        editCalcPace.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (isInternalCalcUpdate) return;
                String input = s.toString().replaceAll("[^\\d]", "");
                if (input.length() > 4) input = input.substring(0, 4);
                StringBuilder formatted = new StringBuilder();
                if (!input.isEmpty()) {
                    if (input.length() <= 2) formatted.append(input);
                    else formatted.append(input.substring(0, input.length() - 2)).append(":").append(input.substring(input.length() - 2));
                }
                isInternalCalcUpdate = true;
                String finalStr = formatted.toString();
                if (!finalStr.equals(s.toString())) {
                    editCalcPace.setText(finalStr);
                    editCalcPace.setSelection(finalStr.length());
                }
                isInternalCalcUpdate = false;
                if (editCalcPace.isFocused()) calculateMissingPaceValue();
            }
        });

        editCalcSpeed.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                if (isInternalCalcUpdate) return;
                if (editCalcSpeed.isFocused()) calculateMissingPaceValue();
            }
        });
    }

    private void calculateMissingPaceValue() {
        try {
            double distance = 0;
            String selected = spinnerCalcDistance.getSelectedItem().toString();
            if ("5K".equalsIgnoreCase(selected)) distance = 5.0;
            else if ("10K".equalsIgnoreCase(selected)) distance = 10.0;
            else if ("Half Marathon".equalsIgnoreCase(selected)) distance = 21.0975;
            else if ("Marathon".equalsIgnoreCase(selected)) distance = 42.195;
            else if ("Custom".equalsIgnoreCase(selected)) {
                String dStr = editCalcCustomDist.getText().toString();
                if (!dStr.isEmpty()) distance = Double.parseDouble(dStr.replace(",", "."));
            }
            if (distance <= 0) return;

            if (editCalcSpeed.isFocused()) {
                String speedStr = editCalcSpeed.getText().toString();
                if (!speedStr.isEmpty()) {
                    double speed = Double.parseDouble(speedStr.replace(",", "."));
                    if (speed > 0) {
                        double paceDecimal = 60.0 / speed;
                        int pm = (int) paceDecimal; int ps = (int) Math.round((paceDecimal - pm) * 60);
                        if (ps == 60) { pm++; ps = 0; }
                        String paceStr = String.format(Locale.getDefault(), "%02d:%02d", pm, ps);
                        
                        if (!paceStr.equals(editCalcPace.getText().toString())) {
                            isInternalCalcUpdate = true; editCalcPace.setText(paceStr); isInternalCalcUpdate = false;
                        }

                        double totalMinutes = distance * paceDecimal;
                        int h = (int) (totalMinutes / 60); int m = (int) (totalMinutes % 60); int s = (int) Math.round((totalMinutes * 60) % 60);
                        if (s == 60) { m++; s = 0; } if (m == 60) { h++; m = 0; }
                        String timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
                        
                        if (!timeStr.equals(editCalcTime.getText().toString())) {
                            isInternalCalcUpdate = true; editCalcTime.setText(timeStr); isInternalCalcUpdate = false;
                        }
                    }
                }
            } else if (editCalcTime.isFocused() || (!editCalcPace.isFocused() && !editCalcSpeed.isFocused())) {
                String timeStr = editCalcTime.getText().toString();
                if (timeStr.contains(":")) {
                    String[] parts = timeStr.split(":");
                    double totalMinutes = 0;
                    if (parts.length == 3) totalMinutes = (Integer.parseInt(parts[0]) * 60) + Integer.parseInt(parts[1]) + (Double.parseDouble(parts[2]) / 60.0);
                    else if (parts.length == 2) totalMinutes = Integer.parseInt(parts[0]) + (Double.parseDouble(parts[1]) / 60.0);
                    if (totalMinutes > 0) {
                        double paceDecimal = totalMinutes / distance;
                        int pm = (int) paceDecimal; int ps = (int) Math.round((paceDecimal - pm) * 60);
                        if (ps == 60) { pm++; ps = 0; }
                        String paceStr = String.format(Locale.getDefault(), "%02d:%02d", pm, ps);
                        if (!paceStr.equals(editCalcPace.getText().toString())) {
                            isInternalCalcUpdate = true; editCalcPace.setText(paceStr); isInternalCalcUpdate = false;
                        }

                        double speed = 60.0 / paceDecimal;
                        String speedStr = String.format(Locale.getDefault(), "%.2f", speed);
                        if (!speedStr.equals(editCalcSpeed.getText().toString())) {
                            isInternalCalcUpdate = true; editCalcSpeed.setText(speedStr); isInternalCalcUpdate = false;
                        }
                    }
                }
            } else if (editCalcPace.isFocused()) {
                String paceStr = editCalcPace.getText().toString();
                if (paceStr.contains(":")) {
                    String[] parts = paceStr.split(":");
                    double paceMinutes = Integer.parseInt(parts[0]) + (Double.parseDouble(parts[1]) / 60.0);
                    if (paceMinutes > 0) {
                        double totalMinutes = distance * paceMinutes;
                        int h = (int) (totalMinutes / 60); int m = (int) (totalMinutes % 60); int s = (int) Math.round((totalMinutes * 60) % 60);
                        if (s == 60) { m++; s = 0; } if (m == 60) { h++; m = 0; }
                        String timeStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
                        if (!timeStr.equals(editCalcTime.getText().toString())) {
                            isInternalCalcUpdate = true; editCalcTime.setText(timeStr); isInternalCalcUpdate = false;
                        }

                        double speed = 60.0 / paceMinutes;
                        String speedStr = String.format(Locale.getDefault(), "%.2f", speed);
                        if (!speedStr.equals(editCalcSpeed.getText().toString())) {
                            isInternalCalcUpdate = true; editCalcSpeed.setText(speedStr); isInternalCalcUpdate = false;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void loadLatestActivity() {
        Executors.newSingleThreadExecutor().execute(() -> {
            WorkoutEntity latest = workoutDao.getLatestCompletedWorkout();
            if (latest != null) {
                latestWorkoutId = latest.getId();
                runOnUiThread(() -> {
                    labelLatestActivity.setVisibility(View.VISIBLE);
                    cardLatestActivity.setVisibility(View.VISIBLE);
                    
                    SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
                    textLatestUserName.setText(prefs.getString("userNickname", prefs.getString("userName", "Runner")));
                    
                    String timeAgo;
                    if (isSameDay(latest.getScheduledDate(), System.currentTimeMillis())) {
                        timeAgo = "Today";
                    } else if (isYesterday(latest.getScheduledDate())) {
                        timeAgo = "Yesterday";
                    } else {
                        long diff = System.currentTimeMillis() - latest.getScheduledDate();
                        long days = diff / 86400000;
                        if (days <= 1) {
                            timeAgo = "Yesterday";
                        } else {
                            timeAgo = days + "d ago";
                        }
                    }
                    textLatestDate.setText(timeAgo);
                    
                    textLatestType.setText("Latest activity: " + WorkoutUtils.getWorkoutTypeWithIcon(latest.getWorkoutType()));
                    
                    if ("INTERVALS".equalsIgnoreCase(latest.getWorkoutType()) && latest.getIntervalCount() > 0) {
                        textLatestDistance.setText(String.valueOf(latest.getIntervalCount()));
                        labelLatestDist.setText("Sets");
                        textLatestDuration.setText(latest.getIntervalValue());
                        labelLatestDur.setText("Work");
                        textLatestPace.setText(latest.getIntervalPace());
                        labelLatestPace.setText("Pace");
                    } else {
                        textLatestDistance.setText(String.format(Locale.getDefault(), "%.2f", latest.getDistance()));
                        labelLatestDist.setText("km");
                        double dur = latest.getTotalDuration();
                        int h = (int) (dur / 60); int m = (int) (dur % 60); int s = (int) Math.round((dur * 60) % 60);
                        if (h > 0) textLatestDuration.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s));
                        else textLatestDuration.setText(String.format(Locale.getDefault(), "%02d:%02d", m, s));
                        labelLatestDur.setText("Time");
                        double pace = latest.getPace();
                        int pm = (int) pace; int ps = (int) Math.round((pace - pm) * 60);
                        textLatestPace.setText(String.format(Locale.getDefault(), "%02d:%02d", pm, ps));
                        labelLatestPace.setText("/km");
                    }

                    if (latest.getAvgHeartRate() > 0) textLatestHR.setText(String.valueOf(latest.getAvgHeartRate()));
                    else textLatestHR.setText("--");

                    loadLatestUserImage();
                });
            } else {
                runOnUiThread(() -> {
                    labelLatestActivity.setVisibility(View.GONE);
                    cardLatestActivity.setVisibility(View.GONE);
                });
            }
        });
    }

    private void loadLatestUserImage() {
        try {
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
            String path = prefs.getString("profileImagePath", null);
            if (path != null && new File(path).exists()) {
                Bitmap b = BitmapFactory.decodeFile(path);
                imageLatestUser.setImageBitmap(b);
                imageLatestUser.setPadding(0, 0, 0, 0);
                imageLatestUser.setImageTintList(null);
                imageLatestUser.setColorFilter(null);
            } else {
                imageLatestUser.setImageResource(R.drawable.ic_person);
                imageLatestUser.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
                imageLatestUser.setPadding(spToPx(4), spToPx(4), spToPx(4), spToPx(4));
            }
        } catch (Exception ignored) {}
    }

    private void loadProfileImage() {
        try {
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
            String path = prefs.getString("profileImagePath", null);
            if (path != null && new File(path).exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4; // More downsampling for small thumbnails
                Bitmap b = BitmapFactory.decodeFile(path, options);
                
                if (b != null) {
                    imageProfileNav.setImageBitmap(b);
                    imageProfileNav.setPadding(0, 0, 0, 0);
                    imageProfileNav.setImageTintList(null);
                    imageProfileNav.setColorFilter(null);
                }
            } else {
                imageProfileNav.setImageResource(R.drawable.ic_person);
                imageProfileNav.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
                imageProfileNav.setPadding(spToPx(4), spToPx(4), spToPx(4), spToPx(4));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRaceInfo() {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        String eventName = prefs.getString("eventName", "My Training Plan");
        long eventDateMillis = prefs.getLong("eventDate", 0);
        String eventType = prefs.getString("eventType", "Marathon");
        String location = prefs.getString("eventLocation", "");

        textRaceName.setText(eventName);
        textHomeRaceCategory.setText(eventType);
        textHomeRaceLocation.setText(location.isEmpty() ? "" : location);
        
        if (eventDateMillis > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            textRaceDate.setText(sdf.format(eventDateMillis));
        } else {
            textRaceDate.setText("Set your date");
        }
    }

    private void loadProgressAndNext() {
        Executors.newSingleThreadExecutor().execute(() -> {
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
            String currentPlan = prefs.getString("eventName", "");
            List<WorkoutEntity> results;
            if (currentPlan.isEmpty()) {
                results = workoutDao.getAllWorkoutsSync();
            } else {
                results = workoutDao.getWorkoutsByPlan(currentPlan);
                // Fallback to all workouts if plan-specific query returns nothing
                if (results == null || results.isEmpty()) {
                    results = workoutDao.getAllWorkoutsSync();
                }
            }
            
            long planStartDate = prefs.getLong("planStartDate", 0);
            List<WorkoutEntity> filteredWorkouts = new java.util.ArrayList<>();
            if (results != null) {
                for (WorkoutEntity w : results) {
                    if (planStartDate == 0 || w.getScheduledDate() >= planStartDate) {
                        filteredWorkouts.add(w);
                    }
                }
            }
            if (filteredWorkouts.isEmpty() && results != null) {
                filteredWorkouts = results;
            }
            final List<WorkoutEntity> planWorkouts = filteredWorkouts;

            if (planWorkouts == null || planWorkouts.isEmpty()) {
                runOnUiThread(() -> {
                    textNextSessionLabel.setText("GET STARTED");
                    textNextType.setText("No workouts planned");
                    textNextDetails.setText("Generate a plan first");
                    textNextDate.setText("");
                    progressOverall.setProgress(0);
                    textHomeProgressPercent.setText("0% COMPLETED");
                });
                return;
            }

            int completedCount = 0;
            WorkoutEntity displayWorkout = null;
            
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            long todayStart = cal.getTimeInMillis();

            String label = "NEXT SESSION";
            for (WorkoutEntity w : planWorkouts) if (w.isCompleted()) completedCount++;
            for (WorkoutEntity w : planWorkouts) { if (!w.isCompleted() && isBeforeToday(w.getScheduledDate(), todayStart)) { displayWorkout = w; label = "MISSED SESSION"; break; } }
            if (displayWorkout == null) { for (WorkoutEntity w : planWorkouts) { if (!w.isCompleted() && isSameDay(w.getScheduledDate(), todayStart)) { displayWorkout = w; label = "TODAY'S SESSION"; break; } } }
            if (displayWorkout == null) { for (WorkoutEntity w : planWorkouts) { if (!w.isCompleted() && isTomorrow(w.getScheduledDate(), todayStart)) { displayWorkout = w; label = "TOMORROW'S SESSION"; break; } } }
            if (displayWorkout == null) { for (WorkoutEntity w : planWorkouts) { if (!w.isCompleted() && w.getScheduledDate() >= todayStart) { displayWorkout = w; label = "NEXT SESSION"; break; } } }
            if (displayWorkout == null && !planWorkouts.isEmpty()) { displayWorkout = planWorkouts.get(planWorkouts.size() - 1); label = "PLAN COMPLETED!"; }
            int percent = (completedCount * 100) / planWorkouts.size();
            final WorkoutEntity finalDisplay = displayWorkout; final int finalPercent = percent; final String finalLabel = label;
            runOnUiThread(() -> {
                nextWorkoutId = finalDisplay != null ? finalDisplay.getId() : -1;
                progressOverall.setProgress(finalPercent);
                textHomeProgressPercent.setText(finalPercent + "% COMPLETED");
                textNextSessionLabel.setText(finalLabel);
                SimpleDateFormat sdf = new SimpleDateFormat("EEEE, dd MMM", Locale.getDefault());
                if (finalDisplay != null) {
                    textNextType.setText(WorkoutUtils.getWorkoutTypeWithIcon(finalDisplay.getWorkoutType()));
                    if ("INTERVALS".equalsIgnoreCase(finalDisplay.getWorkoutType()) && finalDisplay.getIntervalCount() > 0) { textNextDetails.setText(String.format(Locale.getDefault(), "%dx %s @ %s - %s", finalDisplay.getIntervalCount(), finalDisplay.getIntervalValue(), finalDisplay.getIntervalPace(), finalDisplay.getDescription())); }
                    else { textNextDetails.setText(String.format(Locale.getDefault(), "%.1f km - %s", finalDisplay.getDistance(), finalDisplay.getDescription())); }
                    textNextDate.setText(sdf.format(finalDisplay.getScheduledDate()));
                    if (finalLabel.contains("MISSED")) textNextType.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                    else if (finalLabel.contains("COMPLETED")) textNextType.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                    else textNextType.setTextColor(getResources().getColor(R.color.electric_lime));
                }
            });
        });
    }
    
    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }

    private boolean isSameDay(long time1, long time2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        cal1.setTimeInMillis(time1);
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.setTimeInMillis(time2);
        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
               cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    private boolean isBeforeToday(long time, long todayMidnight) {
        return time < todayMidnight && !isSameDay(time, todayMidnight);
    }

    private boolean isTomorrow(long time, long todayMidnight) {
        java.util.Calendar tomorrow = java.util.Calendar.getInstance();
        tomorrow.setTimeInMillis(todayMidnight);
        tomorrow.add(java.util.Calendar.DAY_OF_YEAR, 1);
        return isSameDay(time, tomorrow.getTimeInMillis());
    }

    private boolean isYesterday(long time) {
        java.util.Calendar yesterday = java.util.Calendar.getInstance();
        yesterday.add(java.util.Calendar.DAY_OF_YEAR, -1);
        java.util.Calendar target = java.util.Calendar.getInstance();
        target.setTimeInMillis(time);
        return yesterday.get(java.util.Calendar.YEAR) == target.get(java.util.Calendar.YEAR) &&
               yesterday.get(java.util.Calendar.DAY_OF_YEAR) == target.get(java.util.Calendar.DAY_OF_YEAR);
    }
}
