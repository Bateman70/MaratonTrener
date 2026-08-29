package com.jostein.maratontrener;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.imageview.ShapeableImageView;
import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;

import android.widget.ImageView;
import android.widget.Button;
import androidx.appcompat.app.AlertDialog;
import android.net.Uri;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;
import org.json.JSONArray;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import android.widget.Toast;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.Set;
import java.util.HashSet;

public class DashboardFragment extends Fragment {

    private DatabaseReference highFivesRef;
    private ValueEventListener highFivesListener;
    private static final Set<String> notifiedSenders = new HashSet<>();
    private boolean isFirstLoad = true;

    private TextView textRaceName, textHomeRaceCategory, textRaceDate, textHomeRaceLocation, textHomeProgressPercent, textNextType, textNextDetails, textNextDate;
    private ProgressBar progressOverall;
    private View cardNextActivity, cardRaceOverview;
    private TextView textNextSessionLabel;
    private ShapeableImageView imageProfileNav;
    private WorkoutDao workoutDao;

    // Weather Widget Views
    private View layoutWeatherWidget;
    private TextView textWeatherLabel, textWeatherTemp;
    private ImageView imageWeatherIcon;

    // Weather caching fields
    private static String cachedLocation = "";
    private static boolean cachedIsRaceDayForecast = false;
    private static String cachedTemp = "";
    private static int cachedWeatherCode = -999;
    private static long cachedTime = 0;
    private static double cachedLat = 0.0;
    private static double cachedLon = 0.0;
    private static String cachedGeocodeLocation = "";
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

    // Riegel Predictor & Splits UI
    private Button btnGenerateSplits;
    private TextView labelTogglePredictor;
    private View layoutPredictorFields;
    private EditText editPredictorRecentDist, editPredictorRecentTime;
    private Button btnApplyPrediction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_dashboard, container, false);

        textRaceName = view.findViewById(R.id.textHomeRaceName);
        textHomeRaceCategory = view.findViewById(R.id.textHomeRaceCategory);
        textRaceDate = view.findViewById(R.id.textHomeRaceDate);
        textHomeRaceLocation = view.findViewById(R.id.textHomeRaceLocation);
        layoutWeatherWidget = view.findViewById(R.id.layoutWeatherWidget);
        textWeatherLabel = view.findViewById(R.id.textWeatherLabel);
        textWeatherTemp = view.findViewById(R.id.textWeatherTemp);
        imageWeatherIcon = view.findViewById(R.id.imageWeatherIcon);
        textHomeProgressPercent = view.findViewById(R.id.textHomeProgressPercent);
        textNextType = view.findViewById(R.id.textNextType);
        textNextDetails = view.findViewById(R.id.textNextDetails);
        textNextDate = view.findViewById(R.id.textNextDate);
        progressOverall = view.findViewById(R.id.progressHomeOverall);
        cardNextActivity = view.findViewById(R.id.cardNextActivity);
        cardRaceOverview = view.findViewById(R.id.cardRaceOverview);
        textNextSessionLabel = view.findViewById(R.id.textNextSessionLabel);
        imageProfileNav = view.findViewById(R.id.imageProfileNav);

        cardLatestActivity = view.findViewById(R.id.cardLatestActivity);
        labelLatestActivity = view.findViewById(R.id.labelLatestActivity);
        textLatestUserName = view.findViewById(R.id.textLatestUserName);
        textLatestDate = view.findViewById(R.id.textLatestDate);
        textLatestType = view.findViewById(R.id.textLatestType);
        textLatestDistance = view.findViewById(R.id.textLatestDistance);
        textLatestDuration = view.findViewById(R.id.textLatestDuration);
        textLatestPace = view.findViewById(R.id.textLatestPace);
        textLatestHR = view.findViewById(R.id.textLatestHR);
        labelLatestDist = view.findViewById(R.id.labelLatestDistance);
        labelLatestDur = view.findViewById(R.id.labelLatestDuration);
        labelLatestPace = view.findViewById(R.id.labelLatestPace);
        imageLatestUser = view.findViewById(R.id.imageLatestUser);

        spinnerCalcDistance = view.findViewById(R.id.spinnerCalcDistance);
        editCalcCustomDist = view.findViewById(R.id.editCalcCustomDist);
        editCalcTime = view.findViewById(R.id.editCalcTime);
        editCalcPace = view.findViewById(R.id.editCalcPace);
        editCalcSpeed = view.findViewById(R.id.editCalcSpeed);
        inputLayoutCustomDist = view.findViewById(R.id.inputLayoutCustomDist);

        btnGenerateSplits = view.findViewById(R.id.btnGenerateSplits);
        labelTogglePredictor = view.findViewById(R.id.labelTogglePredictor);
        layoutPredictorFields = view.findViewById(R.id.layoutPredictorFields);
        editPredictorRecentDist = view.findViewById(R.id.editPredictorRecentDist);
        editPredictorRecentTime = view.findViewById(R.id.editPredictorRecentTime);
        btnApplyPrediction = view.findViewById(R.id.btnApplyPrediction);

        setupPaceCalculator();

        workoutDao = WorkoutDatabase.getDatabase(requireContext()).workoutDao();

        imageProfileNav.setOnClickListener(v -> {
            // If in ViewPager, we might want to switch tab instead of launching activity
            if (getActivity() instanceof MainContainerActivity) {
                ((MainContainerActivity) getActivity()).switchToTab(R.id.nav_profile);
            } else {
                startActivity(new Intent(getActivity(), ProfileActivity.class));
            }
        });

        view.findViewById(R.id.btnNavStart).setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), StartActivity.class);
            intent.putExtra("FROM_DASHBOARD", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
        });

        cardNextActivity.setOnClickListener(v -> {
            if (nextWorkoutId != -1) {
                Intent intent = new Intent(getActivity(), EditWorkoutActivity.class);
                intent.putExtra("WORKOUT_ID", nextWorkoutId);
                startActivity(intent);
            } else {
                if (getActivity() instanceof MainContainerActivity) {
                    ((MainContainerActivity) getActivity()).switchToTab(R.id.nav_log);
                } else {
                    startActivity(new Intent(getActivity(), MainActivity.class));
                }
            }
        });

        cardLatestActivity.setOnClickListener(v -> {
            if (latestWorkoutId != -1) {
                Intent intent = new Intent(getActivity(), EditWorkoutActivity.class);
                intent.putExtra("WORKOUT_ID", latestWorkoutId);
                startActivity(intent);
            }
        });

        cardRaceOverview.setOnClickListener(v -> startActivity(new Intent(getActivity(), RaceInfoActivity.class)));

        View cardNutrition = view.findViewById(R.id.cardNutrition);
        cardNutrition.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), NutritionActivity.class));
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadRaceInfo();
        loadProgressAndNext();
        loadLatestActivity();
        loadProfileImage();
        startListeningForHighFives();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopListeningForHighFives();
    }

    private void startListeningForHighFives() {
        try {
            SharedPreferences buddyPrefs = SecurityUtils.getEncryptedPrefs(requireContext(), "BuddyPrefs");
            String myId = buddyPrefs.getString("my_id", "CH020721");
            if (myId == null) return;

            highFivesRef = FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/")
                    .getReference("profiles")
                    .child(myId)
                    .child("highFives");

            isFirstLoad = true;
            highFivesListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            String senderId = child.getKey();
                            String senderName = child.getValue(String.class);
                            if (senderId != null && senderName != null) {
                                if (isFirstLoad) {
                                    notifiedSenders.add(senderId);
                                } else {
                                    if (!notifiedSenders.contains(senderId)) {
                                        notifiedSenders.add(senderId);
                                        if (getContext() != null) {
                                            Toast.makeText(getContext(), senderName + " sent you a High-Five! 🙌", Toast.LENGTH_LONG).show();
                                        }
                                    }
                                }
                            }
                        }
                    }
                    isFirstLoad = false;
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {}
            };
            highFivesRef.addValueEventListener(highFivesListener);
        } catch (Exception ignored) {}
    }

    private void stopListeningForHighFives() {
        if (highFivesRef != null && highFivesListener != null) {
            try {
                highFivesRef.removeEventListener(highFivesListener);
            } catch (Exception ignored) {}
        }
    }

    private void setupPaceCalculator() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
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

        // Splits Generator Button
        btnGenerateSplits.setOnClickListener(v -> showSplitsDialog());

        // Togglable Predictor Header
        labelTogglePredictor.setOnClickListener(v -> {
            if (layoutPredictorFields.getVisibility() == View.VISIBLE) {
                layoutPredictorFields.setVisibility(View.GONE);
                labelTogglePredictor.setText("► RIEGEL PACE PREDICTOR");
            } else {
                layoutPredictorFields.setVisibility(View.VISIBLE);
                labelTogglePredictor.setText("▼ RIEGEL PACE PREDICTOR");
            }
        });

        // Format Predictor Recent Time
        editPredictorRecentTime.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(android.text.Editable s) {
                String input = s.toString().replaceAll("[^\\d]", "");
                if (input.length() > 6) input = input.substring(0, 6);
                StringBuilder formatted = new StringBuilder();
                if (!input.isEmpty()) {
                    if (input.length() <= 2) formatted.append(input);
                    else if (input.length() <= 4) formatted.append(input.substring(0, input.length() - 2)).append(":").append(input.substring(input.length() - 2));
                    else formatted.append(input.substring(0, input.length() - 4)).append(":").append(input.substring(input.length() - 4, input.length() - 2)).append(":").append(input.substring(input.length() - 2));
                }
                String finalStr = formatted.toString();
                if (!finalStr.equals(s.toString())) {
                    editPredictorRecentTime.setText(finalStr);
                    editPredictorRecentTime.setSelection(finalStr.length());
                }
            }
        });

        // Apply Riegel Prediction
        btnApplyPrediction.setOnClickListener(v -> {
            String dist1Str = editPredictorRecentDist.getText().toString();
            String time1Str = editPredictorRecentTime.getText().toString();
            if (dist1Str.isEmpty() || time1Str.isEmpty()) {
                Toast.makeText(getContext(), "Please enter recent distance and time.", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                double d1 = Double.parseDouble(dist1Str.replace(",", "."));
                double t1Seconds = parseTimeToSeconds(time1Str);
                if (d1 <= 0 || t1Seconds <= 0) return;
                
                double d2 = getCalculatorDistance();
                if (d2 <= 0) {
                    Toast.makeText(getContext(), "Please select/enter target distance first.", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Riegel Formula: T2 = T1 * (D2 / D1)^1.06
                double t2Seconds = t1Seconds * Math.pow(d2 / d1, 1.06);
                
                String formattedT2 = formatSecondsToTime(t2Seconds);
                isInternalCalcUpdate = true;
                editCalcTime.setText(formattedT2);
                isInternalCalcUpdate = false;
                
                calculateMissingPaceValue();
                Toast.makeText(getContext(), "Prediction applied successfully!", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(getContext(), "Invalid numbers.", Toast.LENGTH_SHORT).show();
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

    private double parseTimeToSeconds(String timeStr) {
        if (!timeStr.contains(":")) {
            try { return Double.parseDouble(timeStr) * 60.0; } catch (Exception e) { return 0; }
        }
        String[] parts = timeStr.split(":");
        try {
            if (parts.length == 3) {
                return (Integer.parseInt(parts[0]) * 3600) + (Integer.parseInt(parts[1]) * 60) + Double.parseDouble(parts[2]);
            } else if (parts.length == 2) {
                return (Integer.parseInt(parts[0]) * 60) + Double.parseDouble(parts[1]);
            }
        } catch (Exception ignored) {}
        return 0;
    }

    private String formatSecondsToTime(double totalSeconds) {
        int h = (int) (totalSeconds / 3600);
        int m = (int) ((totalSeconds % 3600) / 60);
        int s = (int) Math.round(totalSeconds % 60);
        if (s == 60) { m++; s = 0; }
        if (m == 60) { h++; m = 0; }
        if (h > 0) {
            return String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s);
        } else {
            return String.format(Locale.getDefault(), "%02d:%02d", m, s);
        }
    }

    private double getCalculatorDistance() {
        double distance = 0;
        String selected = spinnerCalcDistance.getSelectedItem().toString();
        if ("5K".equalsIgnoreCase(selected)) distance = 5.0;
        else if ("10K".equalsIgnoreCase(selected)) distance = 10.0;
        else if ("Half Marathon".equalsIgnoreCase(selected)) distance = 21.0975;
        else if ("Marathon".equalsIgnoreCase(selected)) distance = 42.195;
        else if ("Custom".equalsIgnoreCase(selected)) {
            String dStr = editCalcCustomDist.getText().toString();
            if (!dStr.isEmpty()) {
                try { distance = Double.parseDouble(dStr.replace(",", ".")); } catch (Exception ignored) {}
            }
        }
        return distance;
    }

    private void showSplitsDialog() {
        double d = getCalculatorDistance();
        String timeStr = editCalcTime.getText().toString();
        if (d <= 0 || timeStr.isEmpty()) {
            Toast.makeText(getContext(), "Please select/enter target distance and time first.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        double totalSeconds = parseTimeToSeconds(timeStr);
        if (totalSeconds <= 0) return;
        
        double evenPace = totalSeconds / d; // seconds per km
        
        android.widget.ScrollView scrollView = new android.widget.ScrollView(requireContext());
        android.widget.TableLayout tableLayout = new android.widget.TableLayout(requireContext());
        tableLayout.setPadding(24, 24, 24, 24);
        tableLayout.setStretchAllColumns(true);
        
        android.widget.TableRow headerRow = new android.widget.TableRow(requireContext());
        headerRow.setBackgroundColor(0xFF222222);
        headerRow.setPadding(8, 16, 8, 16);
        
        TextView h1 = new TextView(requireContext()); h1.setText("KM"); h1.setTextColor(0xFFFFFFFF); h1.setTextSize(12); h1.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TextView h2 = new TextView(requireContext()); h2.setText("EVEN SPLIT"); h2.setTextColor(0xFFFFFFFF); h2.setTextSize(12); h2.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        TextView h3 = new TextView(requireContext()); h3.setText("NEG SPLIT"); h3.setTextColor(0xFFFFFFFF); h3.setTextSize(12); h3.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        
        headerRow.addView(h1); headerRow.addView(h2); headerRow.addView(h3);
        tableLayout.addView(headerRow);
        
        int totalKm = (int) Math.floor(d);
        double cumEven = 0;
        double cumNeg = 0;
        
        for (int k = 1; k <= totalKm; k++) {
            cumEven += evenPace;
            
            double negPace = evenPace;
            if (d > 1.0) {
                negPace = evenPace * (1.025 - 0.05 * ((double)(k - 1) / (d - 1)));
            }
            cumNeg += negPace;
            
            android.widget.TableRow row = new android.widget.TableRow(requireContext());
            row.setPadding(8, 12, 8, 12);
            if (k % 2 == 0) row.setBackgroundColor(0x11FFFFFF);
            
            TextView tKm = new TextView(requireContext()); tKm.setText(String.valueOf(k)); tKm.setTextColor(0xFFFFFFFF); tKm.setTextSize(12);
            
            TextView tEven = new TextView(requireContext());
            tEven.setText(formatSecondsToTime(evenPace) + " (" + formatSecondsToTime(cumEven) + ")");
            tEven.setTextColor(0xFFCCCCCC);
            tEven.setTextSize(12);
            
            TextView tNeg = new TextView(requireContext());
            tNeg.setText(formatSecondsToTime(negPace) + " (" + formatSecondsToTime(cumNeg) + ")");
            tNeg.setTextColor(0xFFCCFF00);
            tNeg.setTextSize(12);
            
            row.addView(tKm); row.addView(tEven); row.addView(tNeg);
            tableLayout.addView(row);
        }
        
        if (d > totalKm) {
            double fracDist = d - totalKm;
            double fracEvenTime = fracDist * evenPace;
            cumEven += fracEvenTime;
            
            double remainingNegTime = totalSeconds - cumNeg;
            cumNeg = totalSeconds;
            
            android.widget.TableRow row = new android.widget.TableRow(requireContext());
            row.setPadding(8, 12, 8, 12);
            row.setBackgroundColor(0x22CCFF00);
            
            TextView tKm = new TextView(requireContext()); 
            tKm.setText(String.format(Locale.getDefault(), "%.2f (Finish)", d)); 
            tKm.setTextColor(0xFFFFFFFF); 
            tKm.setTextSize(12);
            tKm.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            
            TextView tEven = new TextView(requireContext());
            tEven.setText(formatSecondsToTime(evenPace) + " (" + formatSecondsToTime(cumEven) + ")");
            tEven.setTextColor(0xFFFFFFFF);
            tEven.setTextSize(12);
            tEven.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            
            double finalNegSegmentPace = remainingNegTime / fracDist;
            TextView tNeg = new TextView(requireContext());
            tNeg.setText(formatSecondsToTime(finalNegSegmentPace) + " (" + formatSecondsToTime(cumNeg) + ")");
            tNeg.setTextColor(0xFFCCFF00);
            tNeg.setTextSize(12);
            tNeg.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            
            row.addView(tKm); row.addView(tEven); row.addView(tNeg);
            tableLayout.addView(row);
        }
        
        scrollView.addView(tableLayout);
        
        new AlertDialog.Builder(requireContext(), R.style.CustomDatePickerDialogTheme)
            .setTitle("Race Split Card (Even vs. Neg)")
            .setView(scrollView)
            .setPositiveButton("Close", null)
            .show();
    }

    private void loadLatestActivity() {
        Executors.newSingleThreadExecutor().execute(() -> {
            WorkoutEntity latest = workoutDao.getLatestCompletedWorkout();
            if (latest != null) {
                latestWorkoutId = latest.getId();
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    labelLatestActivity.setVisibility(View.VISIBLE);
                    cardLatestActivity.setVisibility(View.VISIBLE);
                    SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext());
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
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    labelLatestActivity.setVisibility(View.GONE);
                    cardLatestActivity.setVisibility(View.GONE);
                });
            }
        });
    }

    private void loadLatestUserImage() {
        try {
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext());
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
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext());
            String path = prefs.getString("profileImagePath", null);
            if (path != null && new File(path).exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
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
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext());
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

        // Update weather forecast widget
        updateWeatherForecast(location, eventDateMillis);
    }

    private int getWeatherIconResource(int wmoCode) {
        if (wmoCode == 0 || wmoCode == 1) return R.drawable.ic_weather_sunny;
        if (wmoCode == 2) return R.drawable.ic_weather_cloudy_sun;
        if (wmoCode == 3) return R.drawable.ic_weather_cloudy;
        if (wmoCode == 45 || wmoCode == 48) return R.drawable.ic_weather_foggy;
        if (wmoCode >= 51 && wmoCode <= 57) return R.drawable.ic_weather_rainy;
        if (wmoCode >= 61 && wmoCode <= 67) return R.drawable.ic_weather_rainy;
        if (wmoCode >= 80 && wmoCode <= 82) return R.drawable.ic_weather_rainy;
        if (wmoCode >= 71 && wmoCode <= 77) return R.drawable.ic_weather_snowy;
        if (wmoCode >= 85 && wmoCode <= 86) return R.drawable.ic_weather_snowy;
        if (wmoCode >= 95 && wmoCode <= 99) return R.drawable.ic_weather_thunderstorm;
        return R.drawable.ic_weather_cloudy_sun; // Default
    }

    private void updateWeatherForecast(final String location, final long eventDateMillis) {
        if (location == null || location.trim().isEmpty() || 
            location.equalsIgnoreCase("location") || 
            location.equalsIgnoreCase("Place, Country") || 
            location.equalsIgnoreCase("Norway") || 
            eventDateMillis == 0) {
            if (layoutWeatherWidget != null) {
                layoutWeatherWidget.setVisibility(View.GONE);
            }
            return;
        }

        // Compute diffDays
        long todayMillis = System.currentTimeMillis();
        Calendar today = Calendar.getInstance();
        today.setTimeInMillis(todayMillis);
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar race = Calendar.getInstance();
        race.setTimeInMillis(eventDateMillis);
        race.set(Calendar.HOUR_OF_DAY, 0);
        race.set(Calendar.MINUTE, 0);
        race.set(Calendar.SECOND, 0);
        race.set(Calendar.MILLISECOND, 0);

        long diffTime = race.getTimeInMillis() - today.getTimeInMillis();
        final long countdownDays = diffTime / (1000 * 60 * 60 * 24);

        if (countdownDays < 0) {
            if (layoutWeatherWidget != null) {
                layoutWeatherWidget.setVisibility(View.GONE);
            }
            return;
        }

        final boolean isRaceDayForecast = (countdownDays <= 3);
        final String labelText = isRaceDayForecast ? "Race Day" : "Currently";
        final String cleanLocation = location.trim();

        // Check memory cache
        long now = System.currentTimeMillis();
        if (cleanLocation.equalsIgnoreCase(cachedLocation) && 
            isRaceDayForecast == cachedIsRaceDayForecast && 
            (now - cachedTime < 3600000) && 
            !cachedTemp.isEmpty()) {
            
            if (layoutWeatherWidget != null) {
                textWeatherLabel.setText(labelText);
                textWeatherTemp.setText(cachedTemp);
                if (cachedWeatherCode != -999) {
                    imageWeatherIcon.setImageResource(getWeatherIconResource(cachedWeatherCode));
                }
                layoutWeatherWidget.setVisibility(View.VISIBLE);
            }
            return;
        }

        // Run background thread API fetches
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                double lat = cachedLat;
                double lon = cachedLon;
                
                // Step 1: Geocode if location string changed
                if (!cleanLocation.equalsIgnoreCase(cachedGeocodeLocation) || lat == 0.0) {
                    String query = Uri.encode(cleanLocation);
                    String geocodeUrlStr = "https://geocoding-api.open-meteo.com/v1/search?name=" + query + "&count=1&language=en&format=json";
                    URL url = new URL(geocodeUrlStr);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(5000);
                    conn.setReadTimeout(5000);
                    
                    if (conn.getResponseCode() == 200) {
                        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String inputLine;
                        while ((inputLine = in.readLine()) != null) {
                            response.append(inputLine);
                        }
                        in.close();
                        
                        JSONObject json = new JSONObject(response.toString());
                        if (json.has("results")) {
                            JSONArray results = json.getJSONArray("results");
                            if (results.length() > 0) {
                                JSONObject bestMatch = results.getJSONObject(0);
                                lat = bestMatch.getDouble("latitude");
                                lon = bestMatch.getDouble("longitude");
                                cachedLat = lat;
                                cachedLon = lon;
                                cachedGeocodeLocation = cleanLocation;
                            } else {
                                if (getActivity() != null) {
                                    getActivity().runOnUiThread(() -> {
                                        if (layoutWeatherWidget != null) layoutWeatherWidget.setVisibility(View.GONE);
                                    });
                                }
                                return;
                            }
                        } else {
                            if (getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    if (layoutWeatherWidget != null) layoutWeatherWidget.setVisibility(View.GONE);
                                });
                            }
                            return;
                        }
                    } else {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (layoutWeatherWidget != null) layoutWeatherWidget.setVisibility(View.GONE);
                            });
                        }
                        return;
                    }
                }

                // Step 2: Query weather
                String weatherUrlStr = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&timezone=auto";
                if (isRaceDayForecast) {
                    weatherUrlStr += "&daily=weather_code,temperature_2m_max,temperature_2m_min";
                } else {
                    weatherUrlStr += "&current=temperature_2m,weather_code";
                }

                URL wUrl = new URL(weatherUrlStr);
                HttpURLConnection wConn = (HttpURLConnection) wUrl.openConnection();
                wConn.setRequestMethod("GET");
                wConn.setConnectTimeout(5000);
                wConn.setReadTimeout(5000);

                if (wConn.getResponseCode() == 200) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(wConn.getInputStream()));
                    StringBuilder wResponse = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        wResponse.append(inputLine);
                    }
                    in.close();

                    JSONObject wJson = new JSONObject(wResponse.toString());
                    String tempStr = "";
                    int weatherCode = -999;

                    if (isRaceDayForecast) {
                        if (wJson.has("daily")) {
                            JSONObject daily = wJson.getJSONObject("daily");
                            JSONArray timeArray = daily.getJSONArray("time");
                            
                            SimpleDateFormat isoSdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                            String raceDateISO = isoSdf.format(new Date(eventDateMillis));
                            
                            int idx = -1;
                            for (int i = 0; i < timeArray.length(); i++) {
                                if (timeArray.getString(i).equals(raceDateISO)) {
                                    idx = i;
                                    break;
                                }
                            }
                            
                            if (idx != -1) {
                                int minTemp = (int) Math.round(daily.getJSONArray("temperature_2m_min").getDouble(idx));
                                int maxTemp = (int) Math.round(daily.getJSONArray("temperature_2m_max").getDouble(idx));
                                weatherCode = daily.getJSONArray("weather_code").getInt(idx);
                                tempStr = minTemp + "° / " + maxTemp + "°C";
                            } else {
                                // Fallback to current
                                String curUrlStr = "https://api.open-meteo.com/v1/forecast?latitude=" + lat + "&longitude=" + lon + "&current=temperature_2m,weather_code&timezone=auto";
                                URL cUrl = new URL(curUrlStr);
                                HttpURLConnection cConn = (HttpURLConnection) cUrl.openConnection();
                                if (cConn.getResponseCode() == 200) {
                                    BufferedReader cIn = new BufferedReader(new InputStreamReader(cConn.getInputStream()));
                                    StringBuilder cResponse = new StringBuilder();
                                    while ((inputLine = cIn.readLine()) != null) {
                                        cResponse.append(inputLine);
                                    }
                                    cIn.close();
                                    JSONObject cJson = new JSONObject(cResponse.toString());
                                    if (cJson.has("current")) {
                                        JSONObject current = cJson.getJSONObject("current");
                                        int temp = (int) Math.round(current.getDouble("temperature_2m"));
                                        weatherCode = current.getInt("weather_code");
                                        tempStr = temp + "°C";
                                    }
                                }
                            }
                        }
                    } else {
                        if (wJson.has("current")) {
                            JSONObject current = wJson.getJSONObject("current");
                            int temp = (int) Math.round(current.getDouble("temperature_2m"));
                            weatherCode = current.getInt("weather_code");
                            tempStr = temp + "°C";
                        }
                    }

                    if (!tempStr.isEmpty()) {
                        final String finalTemp = tempStr;
                        final int finalCode = weatherCode;
                        
                        // Update cache
                        cachedLocation = cleanLocation;
                        cachedIsRaceDayForecast = isRaceDayForecast;
                        cachedTemp = finalTemp;
                        cachedWeatherCode = finalCode;
                        cachedTime = System.currentTimeMillis();

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (layoutWeatherWidget != null) {
                                    textWeatherLabel.setText(labelText);
                                    textWeatherTemp.setText(finalTemp);
                                    if (finalCode != -999) {
                                        imageWeatherIcon.setImageResource(getWeatherIconResource(finalCode));
                                    }
                                    layoutWeatherWidget.setVisibility(View.VISIBLE);
                                }
                            });
                        }
                    } else {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (layoutWeatherWidget != null) layoutWeatherWidget.setVisibility(View.GONE);
                            });
                        }
                    }
                } else {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (layoutWeatherWidget != null) layoutWeatherWidget.setVisibility(View.GONE);
                        });
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (layoutWeatherWidget != null) layoutWeatherWidget.setVisibility(View.GONE);
                    });
                }
            }
        });
    }

    private void loadProgressAndNext() {
        Executors.newSingleThreadExecutor().execute(() -> {
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext());
            String currentPlan = prefs.getString("eventName", "");
            List<WorkoutEntity> workouts;
            if (currentPlan.isEmpty()) workouts = workoutDao.getAllWorkoutsSync();
            else {
                workouts = workoutDao.getWorkoutsByPlan(currentPlan);
                if (workouts == null || workouts.isEmpty()) workouts = workoutDao.getAllWorkoutsSync();
            }
            
            long planStartDate = prefs.getLong("planStartDate", 0);
            List<WorkoutEntity> filteredWorkouts = new java.util.ArrayList<>();
            for (WorkoutEntity w : workouts) {
                if (planStartDate == 0 || w.getScheduledDate() >= planStartDate) {
                    filteredWorkouts.add(w);
                }
            }
            if (filteredWorkouts.isEmpty()) {
                filteredWorkouts = workouts;
            }
            final List<WorkoutEntity> planWorkouts = filteredWorkouts;

            if (planWorkouts.isEmpty()) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
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
            getActivity().runOnUiThread(() -> {
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