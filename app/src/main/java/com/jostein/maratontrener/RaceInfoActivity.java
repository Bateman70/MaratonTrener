package com.jostein.maratontrener;

import android.app.DatePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.Editable;
import android.text.TextWatcher;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import android.widget.LinearLayout;
import java.util.List;
import java.util.ArrayList;

public class RaceInfoActivity extends AppCompatActivity {

    private EditText editRaceName, editRaceDate, editRaceLocation, editGoalTime, editRaceUrl, editRaceNotes;
    private Spinner spinnerRaceCategory;
    private TextView textGoalPace;
    private Button btnOpenUrl, btnSaveRaceInfo;
    private long selectedDateMillis;
    private boolean hasUnsavedChanges = false;
    private boolean isInternalUpdate = false;
    private long activityStartTime = 0;

    // GPX Route Fields
    private LinearLayout layoutGpxStatus;
    private TextView textGpxStatus;
    private Button btnRemoveGpx, btnUploadGpx, btnViewRouteMap;
    private String currentGpxDataJson = null;
    private String gpxRouteName = "";
    private double gpxDistance = 0.0;
    private double gpxElevationGain = 0.0;
    private double gpxAvgSlope = 0.0;
    private boolean isGpxChanged = false;
    private static final int PICK_GPX_FILE = 221;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race_info);
        activityStartTime = System.currentTimeMillis();

        Toolbar toolbar = findViewById(R.id.toolbarRaceInfo);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        findViewById(R.id.btnBackRaceInfo).setOnClickListener(v -> checkUnsavedChanges());

        editRaceName = findViewById(R.id.editRaceName);
        editRaceDate = findViewById(R.id.editRaceDate);
        editRaceLocation = findViewById(R.id.editRaceLocation);
        spinnerRaceCategory = findViewById(R.id.spinnerRaceCategory);
        editGoalTime = findViewById(R.id.editGoalTime);
        textGoalPace = findViewById(R.id.textGoalPace);
        editRaceUrl = findViewById(R.id.editRaceUrl);
        editRaceNotes = findViewById(R.id.editRaceNotes);
        btnOpenUrl = findViewById(R.id.btnOpenUrl);
        btnSaveRaceInfo = findViewById(R.id.btnSaveRaceInfo);

        // GPX UI Bindings & Listeners
        layoutGpxStatus = findViewById(R.id.layoutGpxStatus);
        textGpxStatus = findViewById(R.id.textGpxStatus);
        btnRemoveGpx = findViewById(R.id.btnRemoveGpx);
        btnUploadGpx = findViewById(R.id.btnUploadGpx);
        btnViewRouteMap = findViewById(R.id.btnViewRouteMap);

        btnUploadGpx.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(Intent.createChooser(intent, "Select GPX File"), PICK_GPX_FILE);
        });
        btnRemoveGpx.setOnClickListener(v -> removeGpxRoute());
        btnViewRouteMap.setOnClickListener(v -> {
            Intent mapIntent = new Intent(this, RouteViewerActivity.class);
            startActivity(mapIntent);
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.event_types, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item);
        spinnerRaceCategory.setAdapter(adapter);

        findViewById(R.id.imageProfileRaceInfo).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        editRaceDate.setOnClickListener(v -> showDatePicker());

        loadRaceData();
        loadProfileImage();
        setupGoalTimeAutoFormat();
        setupChangeTracking();

        btnSaveRaceInfo.setOnClickListener(v -> saveRaceData());

        btnOpenUrl.setOnClickListener(v -> {
            String url = editRaceUrl.getText().toString().trim();
            if (!url.isEmpty()) {
                if (!url.startsWith("http://") && !url.startsWith("https://")) url = "http://" + url;
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } else {
                Toast.makeText(this, R.string.enter_url_first, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfileImage();
    }

    private void setupChangeTracking() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isInternalUpdate || (System.currentTimeMillis() - activityStartTime < 1000)) return;
                View focused = getCurrentFocus();
                if (focused instanceof EditText) {
                    hasUnsavedChanges = true;
                }
            }
        };
        editRaceName.addTextChangedListener(watcher);
        editRaceLocation.addTextChangedListener(watcher);
        editGoalTime.addTextChangedListener(watcher);
        editRaceUrl.addTextChangedListener(watcher);
        editRaceNotes.addTextChangedListener(watcher);
        
        spinnerRaceCategory.setOnTouchListener((v, event) -> {
            v.performClick();
            if (!isInternalUpdate && (System.currentTimeMillis() - activityStartTime > 1000)) {
                hasUnsavedChanges = true;
            }
            return false;
        });
        
        spinnerRaceCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateGoalPace(editGoalTime.getText().toString(), parent.getItemAtPosition(position).toString());
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
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

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        if (selectedDateMillis > 0) calendar.setTimeInMillis(selectedDateMillis);
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(year, month, dayOfMonth);
            selectedDateMillis = calendar.getTimeInMillis();
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            editRaceDate.setText(sdf.format(calendar.getTime()));
            if (!isInternalUpdate) hasUnsavedChanges = true;
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void loadRaceData() {
        isInternalUpdate = true;
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        String eventName = prefs.getString("eventName", "My Training Plan");
        selectedDateMillis = prefs.getLong("eventDate", 0);
        String targetTime = prefs.getString("targetTime", "04:00:00");
        String eventType = prefs.getString("eventType", "Marathon");
        String location = prefs.getString("eventLocation", "");

        editRaceName.setText(eventName);
        if (selectedDateMillis > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
            editRaceDate.setText(sdf.format(selectedDateMillis));
        }

        if (eventType != null) {
            ArrayAdapter adapter = (ArrayAdapter) spinnerRaceCategory.getAdapter();
            int pos = adapter.getPosition(eventType);
            if (pos >= 0) spinnerRaceCategory.setSelection(pos);
        }
        
        editRaceLocation.setText(location);
        editGoalTime.setText(targetTime);
        updateGoalPace(targetTime, eventType);

        editRaceUrl.setText(prefs.getString("raceUrl_" + eventName, ""));
        editRaceNotes.setText(prefs.getString("raceNotes_" + eventName, ""));
        
        // Load GPX Route Info
        currentGpxDataJson = prefs.getString("gpxRoutePoints", null);
        gpxRouteName = prefs.getString("gpxRouteName", "");
        gpxDistance = Double.longBitsToDouble(prefs.getLong("gpxDistance", Double.doubleToRawLongBits(0.0)));
        gpxElevationGain = Double.longBitsToDouble(prefs.getLong("gpxElevationGain", Double.doubleToRawLongBits(0.0)));
        gpxAvgSlope = Double.longBitsToDouble(prefs.getLong("gpxAvgSlope", Double.doubleToRawLongBits(0.0)));
        updateGpxUI();
        
        editRaceName.postDelayed(() -> {
            isInternalUpdate = false;
            hasUnsavedChanges = false;
        }, 300);
    }

    private void updateGoalPace(String targetTime, String eventType) {
        double distance = 42.2;
        if (eventType != null) {
            String lower = eventType.toLowerCase();
            if (lower.contains("half")) distance = 21.1;
            else if (lower.contains("10k")) distance = 10.0;
            else if (lower.contains("5k")) distance = 5.0;
            else if (lower.contains("10mile")) distance = 16.09;
        }

        try {
            String[] parts = targetTime.split(":");
            double totalMinutes = 0;
            if (parts.length == 3) {
                totalMinutes = (Integer.parseInt(parts[0]) * 60.0) + Integer.parseInt(parts[1]) + (Integer.parseInt(parts[2]) / 60.0);
            } else if (parts.length == 2) {
                totalMinutes = Integer.parseInt(parts[0]) + (Integer.parseInt(parts[1]) / 60.0);
            }
            if (totalMinutes > 0) {
                double pace = totalMinutes / distance;
                int paceMin = (int) pace;
                int paceSec = (int) Math.round((pace - paceMin) * 60);
                textGoalPace.setText(String.format(Locale.getDefault(), "%d:%02d min/km", paceMin, paceSec));
            }
        } catch (Exception e) {
            textGoalPace.setText("--");
        }
    }

    private void setupGoalTimeAutoFormat() {
        editGoalTime.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                String input = s.toString().replaceAll("[^\\d]", "");
                if (input.length() > 6) input = input.substring(0, 6);
                StringBuilder formatted = new StringBuilder();
                if (!input.isEmpty()) {
                    if (input.length() <= 2) formatted.append(input);
                    else if (input.length() <= 4) formatted.append(input.substring(0, input.length()-2)).append(":").append(input.substring(input.length()-2));
                    else formatted.append(input.substring(0, input.length()-4)).append(":").append(input.substring(input.length()-4, input.length()-2)).append(":").append(input.substring(input.length()-2));
                }
                isUpdating = true; editGoalTime.setText(formatted.toString()); editGoalTime.setSelection(formatted.length()); isUpdating = false;
                updateGoalPace(formatted.toString(), spinnerRaceCategory.getSelectedItem().toString());
            }
        });
    }

    private void saveRaceData() {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        String name = editRaceName.getText().toString().trim();
        String category = spinnerRaceCategory.getSelectedItem().toString();
        String location = editRaceLocation.getText().toString().trim();
        
        if (name.isEmpty()) { Toast.makeText(this, "Please enter a race name", Toast.LENGTH_SHORT).show(); return; }
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("eventName", name)
            .putLong("eventDate", selectedDateMillis)
            .putString("eventType", category)
            .putString("eventLocation", location)
            .putString("targetTime", editGoalTime.getText().toString())
            .putString("raceUrl_" + name, editRaceUrl.getText().toString().trim())
            .putString("raceNotes_" + name, editRaceNotes.getText().toString().trim())
            .putLong("profileLastUpdate", System.currentTimeMillis());
            
        // Save GPX Details
        if (currentGpxDataJson != null) {
            editor.putString("gpxRoutePoints", currentGpxDataJson);
            editor.putString("gpxRouteName", gpxRouteName);
            editor.putLong("gpxDistance", Double.doubleToRawLongBits(gpxDistance));
            editor.putLong("gpxElevationGain", Double.doubleToRawLongBits(gpxElevationGain));
            editor.putLong("gpxAvgSlope", Double.doubleToRawLongBits(gpxAvgSlope));
        } else {
            editor.remove("gpxRoutePoints");
            editor.remove("gpxRouteName");
            editor.remove("gpxDistance");
            editor.remove("gpxElevationGain");
            editor.remove("gpxAvgSlope");
        }
        editor.apply();
        
        WorkoutUtils.uploadWorkoutsToFirebase(getApplicationContext());
        hasUnsavedChanges = false;
        Toast.makeText(this, R.string.race_info_saved, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void loadProfileImage() {
        try {
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
            String path = prefs.getString("profileImagePath", null);
            com.google.android.material.imageview.ShapeableImageView profileImage = findViewById(R.id.imageProfileRaceInfo);
            if (path != null && new File(path).exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                Bitmap b = BitmapFactory.decodeFile(path, options);
                if (b != null) {
                    profileImage.setImageBitmap(b); 
                    profileImage.setPadding(0, 0, 0, 0); 
                    profileImage.setImageTintList(null); 
                    profileImage.setColorFilter(null);
                }
            } else {
                profileImage.setImageResource(R.drawable.ic_person); 
                profileImage.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
                profileImage.setPadding(spToPx(4), spToPx(4), spToPx(4), spToPx(4));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }

    private void updateGpxUI() {
        if (currentGpxDataJson != null && !currentGpxDataJson.isEmpty()) {
            layoutGpxStatus.setVisibility(View.VISIBLE);
            textGpxStatus.setText(String.format(Locale.getDefault(), "Route: %s (%.1f km)", gpxRouteName, gpxDistance));
            btnViewRouteMap.setVisibility(View.VISIBLE);
            btnUploadGpx.setVisibility(View.GONE);
        } else {
            layoutGpxStatus.setVisibility(View.GONE);
            btnViewRouteMap.setVisibility(View.GONE);
            btnUploadGpx.setVisibility(View.VISIBLE);
        }
    }

    private void removeGpxRoute() {
        currentGpxDataJson = null;
        gpxRouteName = "";
        gpxDistance = 0.0;
        gpxElevationGain = 0.0;
        gpxAvgSlope = 0.0;
        isGpxChanged = true;
        hasUnsavedChanges = true;
        updateGpxUI();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_GPX_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri uri = data.getData();
            processGpxFile(uri);
        }
    }

    private void processGpxFile(Uri uri) {
        try {
            java.io.InputStream is = getContentResolver().openInputStream(uri);
            if (is == null) throw new java.io.IOException("Unable to open GPX file");
            
            String fileName = "Loaded Route";
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
                cursor.close();
            }
            if (fileName.contains(".")) {
                fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            }
            
            org.xmlpull.v1.XmlPullParserFactory factory = org.xmlpull.v1.XmlPullParserFactory.newInstance();
            factory.setNamespaceAware(true);
            org.xmlpull.v1.XmlPullParser parser = factory.newPullParser();
            parser.setInput(is, null);
            
            List<TrackPoint> rawPoints = new ArrayList<>();
            int eventType = parser.getEventType();
            TrackPoint currentPt = null;
            
            while (eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                String name = parser.getName();
                if (eventType == org.xmlpull.v1.XmlPullParser.START_TAG) {
                    if ("trkpt".equalsIgnoreCase(name)) {
                        String latStr = parser.getAttributeValue(null, "lat");
                        String lonStr = parser.getAttributeValue(null, "lon");
                        if (latStr != null && lonStr != null) {
                            currentPt = new TrackPoint();
                            currentPt.lat = Double.parseDouble(latStr);
                            currentPt.lon = Double.parseDouble(lonStr);
                        }
                    } else if ("ele".equalsIgnoreCase(name) && currentPt != null) {
                        try {
                            currentPt.ele = Double.parseDouble(parser.nextText());
                        } catch (Exception ignored) {}
                    }
                } else if (eventType == org.xmlpull.v1.XmlPullParser.END_TAG) {
                    if ("trkpt".equalsIgnoreCase(name) && currentPt != null) {
                        rawPoints.add(currentPt);
                        currentPt = null;
                    }
                }
                eventType = parser.next();
            }
            is.close();
            
            if (rawPoints.isEmpty()) {
                Toast.makeText(this, "No valid trackpoints found in GPX", Toast.LENGTH_LONG).show();
                return;
            }
            
            double totalDist = 0.0;
            double totalGain = 0.0;
            List<double[]> processed = new ArrayList<>();
            
            TrackPoint first = rawPoints.get(0);
            processed.add(new double[]{first.lat, first.lon, first.ele, 0.0});
            
            for (int i = 1; i < rawPoints.size(); i++) {
                TrackPoint prev = rawPoints.get(i - 1);
                TrackPoint curr = rawPoints.get(i);
                double d = computeHaversine(prev.lat, prev.lon, curr.lat, curr.lon);
                totalDist += d;
                
                double eleDiff = curr.ele - prev.ele;
                if (eleDiff > 0) {
                    totalGain += eleDiff;
                }
                processed.add(new double[]{curr.lat, curr.lon, curr.ele, totalDist});
            }
            
            int target = 300;
            List<double[]> downsampled = new ArrayList<>();
            downsampled.add(processed.get(0));
            
            if (processed.size() > target) {
                double interval = totalDist / (target - 1);
                double nextTarget = interval;
                for (int i = 1; i < processed.size() - 1; i++) {
                    double[] pt = processed.get(i);
                    if (pt[3] >= nextTarget) {
                        downsampled.add(pt);
                        nextTarget += interval;
                    }
                }
                downsampled.add(processed.get(processed.size() - 1));
            } else {
                for (int i = 1; i < processed.size(); i++) {
                    downsampled.add(processed.get(i));
                }
            }
            
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < downsampled.size(); i++) {
                double[] p = downsampled.get(i);
                sb.append(String.format(Locale.US, "[%.6f,%.6f,%.1f,%.3f]", p[0], p[1], p[2], p[3]));
                if (i < downsampled.size() - 1) sb.append(",");
            }
            sb.append("]");
            
            currentGpxDataJson = sb.toString();
            gpxRouteName = fileName;
            gpxDistance = Math.round(totalDist * 100) / 100.0;
            gpxElevationGain = Math.round(totalGain * 10) / 10.0;
            double avgSlope = totalDist > 0 ? (totalGain / (totalDist * 1000)) * 100 : 0;
            gpxAvgSlope = Math.round(avgSlope * 100) / 100.0;
            
            isGpxChanged = true;
            hasUnsavedChanges = true;
            updateGpxUI();
            Toast.makeText(this, "GPX file loaded successfully!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Failed to parse GPX: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private double computeHaversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    private static class TrackPoint {
        double lat;
        double lon;
        double ele = 0.0;
    }
}
