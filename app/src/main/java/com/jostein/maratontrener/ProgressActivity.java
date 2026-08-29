package com.jostein.maratontrener;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Executors;

public class ProgressActivity extends AppCompatActivity {

    private WorkoutDao workoutDao;
    private LinearLayout chartContainer;
    private TextView consistencyText, distanceText, plannedActivitiesText, missedText, countdownText, paceText, intervalStatText, strengthDoneText, longestRunText, totalRunsText, avgHRRunsText;

    private ShapeableImageView imageProfileStats;
    private BottomNavigationView bottomNavigationView;
    private int longestRunId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_progress);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarStats);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        imageProfileStats = findViewById(R.id.imageProfileStats);
        imageProfileStats.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        chartContainer = findViewById(R.id.chartContainer);
        consistencyText = findViewById(R.id.textConsistency);
        distanceText = findViewById(R.id.textDistance);
        plannedActivitiesText = findViewById(R.id.textPlannedDist);
        missedText = findViewById(R.id.textMissed);
        countdownText = findViewById(R.id.textCountdown);
        paceText = findViewById(R.id.textAveragePace);
        intervalStatText = findViewById(R.id.textIntervalStat);
        strengthDoneText = findViewById(R.id.textStrengthDone);
        longestRunText = findViewById(R.id.textLongestRun);
        totalRunsText = findViewById(R.id.textTotalRuns);
        avgHRRunsText = findViewById(R.id.textAvgHRRuns);
        
        bottomNavigationView = findViewById(R.id.bottomNavigation);
        
        findViewById(R.id.cardLongestRun).setOnClickListener(v -> {
            if (longestRunId != -1) {
                Intent intent = new Intent(this, EditWorkoutActivity.class);
                intent.putExtra("WORKOUT_ID", longestRunId);
                startActivity(intent);
            }
        });

        findViewById(R.id.cardMissed).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra("FILTER_MISSED", true);
            startActivity(intent);
        });

        findViewById(R.id.btnBackStats).setOnClickListener(v -> finish());

        workoutDao = WorkoutDatabase.getDatabase(this).workoutDao();
        setupNavigation();
    }

    private void setupNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_stats);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_stats) return true;
            
            Intent intent = null;
            if (id == R.id.nav_home) intent = new Intent(this, HomeActivity.class);
            else if (id == R.id.nav_log) intent = new Intent(this, MainActivity.class);
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

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.getMenu().findItem(R.id.nav_stats).setChecked(true);
        }
        loadProfileImage();
        refreshStats();
    }

    private void loadProfileImage() {
        try {
            android.content.SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
            String path = prefs.getString("profileImagePath", null);

            if (path != null && new File(path).exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                Bitmap b = BitmapFactory.decodeFile(path, options);
                
                if (b != null) {
                    imageProfileStats.setImageBitmap(b);
                    imageProfileStats.setPadding(0, 0, 0, 0);
                    imageProfileStats.setImageTintList(null);
                    imageProfileStats.setColorFilter(null);
                }
            } else {
                imageProfileStats.setImageResource(R.drawable.ic_person);
                imageProfileStats.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
                imageProfileStats.setPadding(spToPx(4), spToPx(4), spToPx(4), spToPx(4));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void refreshStats() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<WorkoutEntity> allWorkouts = workoutDao.getAllWorkouts();
            runOnUiThread(() -> {
                if (allWorkouts == null || allWorkouts.isEmpty()) {
                    showEmptyState();
                } else {
                    processAndShowStats(allWorkouts);
                }
            });
        });
    }

    private void showEmptyState() {
        if (consistencyText != null) consistencyText.setText("N/A");
        if (distanceText != null) distanceText.setText("0.0 km");
        chartContainer.removeAllViews();
    }

    private void processAndShowStats(List<WorkoutEntity> allWorkouts) {
        int completedTotal = 0;
        int completedToDate = 0;
        int missedToDate = 0;
        int shouldBeCompletedByNow = 0;
        double totalDistance = 0.0;
        double runningPaceSum = 0;
        int runsWithPace = 0;
        
        int intervalsDone = 0, steadyDone = 0, longDone = 0, tempoDone = 0, strengthDone = 0;
        int intervalsTotal = 0, runsTotal = 0, strengthTotal = 0;
        
        double avgHRSum = 0;
        int runsWithHR = 0;
        
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long todayStart = cal.getTimeInMillis();
        long now = System.currentTimeMillis();

        double maxDistance = 0;
        longestRunId = -1;

        java.util.Set<String> completedDays = new java.util.HashSet<>();
        SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        for (WorkoutEntity workout : allWorkouts) {
            if (workout.isCompleted()) {
                completedDays.add(dayFormat.format(new java.util.Date(workout.getScheduledDate())));
            }
        }

        for (WorkoutEntity workout : allWorkouts) {
            String type = workout.getWorkoutType();
            String upperType = (type != null) ? type.toUpperCase().trim() : "";
            boolean isInterval = upperType.contains("INTERVAL");
            boolean isStrength = upperType.contains("STRENGTH") || upperType.contains("CORE");
            boolean isRun = !isStrength && !isInterval;

            if (isInterval) intervalsTotal++;
            else if (isStrength) strengthTotal++;
            else runsTotal++;

            boolean isPast = workout.getScheduledDate() < todayStart;
            boolean isToday = workout.getScheduledDate() >= todayStart && workout.getScheduledDate() < todayStart + 86400000;
            
            if (workout.isCompleted()) {
                completedTotal++;
                
                if (!isInterval && !isStrength) {
                    totalDistance += workout.getDistance();
                    if (workout.getDistance() > maxDistance) {
                        maxDistance = workout.getDistance();
                        longestRunId = workout.getId();
                    }
                    if (workout.getAvgHeartRate() > 0) {
                        avgHRSum += workout.getAvgHeartRate();
                        runsWithHR++;
                    }
                }

                if (isInterval) intervalsDone++;
                else if (upperType.contains("STEADY")) steadyDone++;
                else if (upperType.contains("LONG")) longDone++;
                else if (upperType.contains("TEMPO")) tempoDone++;
                else if (isStrength) strengthDone++;
                
                if (isRun && workout.getPace() > 0) {
                    runsWithPace++;
                    runningPaceSum += workout.getPace();
                }

                if (isPast || isToday) {
                    completedToDate++;
                    shouldBeCompletedByNow++;
                }
            } else {
                String workoutDay = dayFormat.format(new java.util.Date(workout.getScheduledDate()));
                if (!completedDays.contains(workoutDay)) {
                    if (isPast) {
                        missedToDate++;
                        shouldBeCompletedByNow++;
                    }
                }
            }
        }

        double percentToDate = shouldBeCompletedByNow == 0 ? 100 : (100.0 * completedToDate / shouldBeCompletedByNow);
        double avgPace = runsWithPace == 0 ? 0 : (runningPaceSum / runsWithPace);
        double avgHR = runsWithHR == 0 ? 0 : (avgHRSum / runsWithHR);

        int totalActivitiesCount = allWorkouts.size();
        plannedActivitiesText.setText(String.format(Locale.getDefault(), "%d done, %d to go", completedTotal, totalActivitiesCount - completedTotal));
        
        distanceText.setText(String.format(Locale.getDefault(), "%.1f km", totalDistance));
        paceText.setText(formatPace(avgPace) + " min/km");
        longestRunText.setText(String.format(Locale.getDefault(), "%.1f km", maxDistance));

        totalRunsText.setText(String.format(Locale.getDefault(), "%d done, %d to go", (completedTotal - strengthDone - intervalsDone), (runsTotal - (completedTotal - strengthDone - intervalsDone))));
        intervalStatText.setText(String.format(Locale.getDefault(), "%d done, %d to go", intervalsDone, (intervalsTotal - intervalsDone)));
        strengthDoneText.setText(String.format(Locale.getDefault(), "%d done, %d to go", strengthDone, (strengthTotal - strengthDone)));

        consistencyText.setText(String.format(Locale.getDefault(), "%.0f%% (%d/%d)", percentToDate, completedToDate, shouldBeCompletedByNow));
        if (percentToDate >= 90) consistencyText.setTextColor(0xFF34C759); 
        else consistencyText.setTextColor(0xFFFF3B30); 

        missedText.setText(String.format(Locale.getDefault(), "%d activities", missedToDate));
        if (missedToDate > 0) missedText.setTextColor(0xFFFF3B30);
        else missedText.setTextColor(0xFFFFFFFF);

        if (avgHR > 0) {
            String zoneStr = calculateHRZone(avgHR);
            avgHRRunsText.setText(String.format(Locale.getDefault(), "%.0f (%s)", avgHR, zoneStr));
        } else {
            avgHRRunsText.setText("--");
        }

        android.content.SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        long eventDate = prefs.getLong("eventDate", 0);
        long days = 0;
        if (eventDate > 0) {
            Calendar calEvent = Calendar.getInstance();
            calEvent.setTimeInMillis(eventDate);
            calEvent.set(Calendar.HOUR_OF_DAY, 0);
            calEvent.set(Calendar.MINUTE, 0);
            calEvent.set(Calendar.SECOND, 0);
            calEvent.set(Calendar.MILLISECOND, 0);

            days = (calEvent.getTimeInMillis() - now) / (1000 * 60 * 60 * 24);
            if (days < 0) days = 0;
        }
        countdownText.setText(String.format(Locale.getDefault(), "%d DAYS", days));

        chartContainer.removeAllViews();
        // Top Chart: Plan Progress (Completed vs Total)
        double totalProgressPercent = totalActivitiesCount == 0 ? 0 : (100.0 * completedTotal / totalActivitiesCount);
        addPieChart(totalProgressPercent, "Plan Progress");
        
        addTypeBreakdownChart(intervalsDone, steadyDone, longDone, tempoDone, strengthDone);
        addWeeklyBarChart(allWorkouts);
        addMonthlyBarChart(allWorkouts);
        addPaceTrendChart(allWorkouts);
        addHeartRateTrendChart(allWorkouts);
    }

    private String calculateHRZone(double avgHR) {
        android.content.SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        int maxHR = 0;
        String maxStr = prefs.getString("userMaxHR", "");
        if (!maxStr.isEmpty()) {
            try { maxHR = Integer.parseInt(maxStr); } catch (Exception ignored) {}
        } else {
            String ageStr = prefs.getString("userAge", "30");
            try { maxHR = 220 - Integer.parseInt(ageStr); } catch (Exception ignored) {}
        }
        if (maxHR == 0) return "Zone: ?";
        double pct = avgHR / maxHR;
        if (pct >= 0.9) return "Zone: 5";
        if (pct >= 0.8) return "Zone: 4";
        if (pct >= 0.7) return "Zone: 3";
        if (pct >= 0.6) return "Zone: 2";
        return "Zone: 1";
    }

    private String formatPace(double decimalPace) {
        if (decimalPace <= 0) return "0:00";
        int min = (int) decimalPace;
        int sec = (int) Math.round((decimalPace - min) * 60);
        return String.format(Locale.getDefault(), "%d:%02d", min, sec);
    }

    private void addMonthlyBarChart(List<WorkoutEntity> allWorkouts) {
        Map<Integer, Double> monthlyDistance = new TreeMap<>();
        Calendar cal = Calendar.getInstance();
        for (WorkoutEntity w : allWorkouts) {
            if (!w.isCompleted()) continue;
            cal.setTimeInMillis(w.getScheduledDate());
            int key = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.MONTH); 
            Double existing = monthlyDistance.get(key);
            monthlyDistance.put(key, (existing == null ? 0.0 : existing) + w.getDistance());
        }
        if (monthlyDistance.isEmpty()) return;
        addChartTitle("MONTHLY VOLUME (KM)");
        BarChart barChart = new BarChart(this);
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        String[] monthNames = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        int i = 0;
        for (Map.Entry<Integer, Double> entry : monthlyDistance.entrySet()) {
            entries.add(new BarEntry(i, entry.getValue().floatValue()));
            labels.add(monthNames[entry.getKey() % 100]);
            i++;
        }
        setupBarChart(barChart, entries, labels, "Monthly Dist");
        chartContainer.addView(barChart, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, spToPx(200)));
    }

    private void addWeeklyBarChart(List<WorkoutEntity> allWorkouts) {
        Map<Integer, Double> weeklyDistance = new TreeMap<>();
        Calendar cal = Calendar.getInstance();
        for (WorkoutEntity w : allWorkouts) {
            if (!w.isCompleted()) continue;
            cal.setTimeInMillis(w.getScheduledDate());
            int key = cal.get(Calendar.YEAR) * 100 + cal.get(Calendar.WEEK_OF_YEAR); 
            Double existing = weeklyDistance.get(key);
            weeklyDistance.put(key, (existing == null ? 0.0 : existing) + w.getDistance());
        }
        if (weeklyDistance.isEmpty()) return;
        addChartTitle("WEEKLY VOLUME (KM)");
        BarChart barChart = new BarChart(this);
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        int i = 0;
        for (Map.Entry<Integer, Double> entry : weeklyDistance.entrySet()) {
            entries.add(new BarEntry(i, entry.getValue().floatValue()));
            labels.add("W" + (entry.getKey() % 100));
            i++;
        }
        setupBarChart(barChart, entries, labels, "Weekly Dist");
        chartContainer.addView(barChart, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, spToPx(200)));
    }

    private void setupBarChart(BarChart barChart, List<BarEntry> entries, List<String> labels, String label) {
        BarDataSet dataSet = new BarDataSet(entries, label);
        dataSet.setColor(0xFFCCFF00); 
        dataSet.setValueTextColor(0xFFFFFFFF);
        dataSet.setValueTextSize(11f);
        dataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.1f", value);
            }
        });
        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.6f);
        barChart.setData(barData);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setGranularityEnabled(true);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(0xFFFFFFFF);
        xAxis.setTextSize(11f);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        YAxis leftAxis = barChart.getAxisLeft();
        leftAxis.setTextColor(0xFFFFFFFF);
        leftAxis.setTextSize(11f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(0x33FFFFFF);
        leftAxis.setDrawAxisLine(false);
        barChart.getAxisRight().setEnabled(false);
        barChart.getDescription().setEnabled(false);
        barChart.getLegend().setEnabled(false);
        barChart.animateY(1000);
    }

    private void addPaceTrendChart(List<WorkoutEntity> allWorkouts) {
        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        List<WorkoutEntity> sorted = new ArrayList<>(allWorkouts);
        Collections.sort(sorted, (a, b) -> Long.compare(a.getScheduledDate(), b.getScheduledDate()));
        int count = 0;
        for (WorkoutEntity w : sorted) {
            if (w.isCompleted() && w.getPace() > 0) {
                entries.add(new Entry(count, (float) w.getPace()));
                dates.add(sdf.format(new java.util.Date(w.getScheduledDate())));
                count++;
            }
        }
        if (entries.isEmpty()) return;
        addChartTitle("OVERALL PACE TREND");
        LineChart lineChart = new LineChart(this);
        LineDataSet dataSet = new LineDataSet(entries, "Pace");
        dataSet.setColor(0xFF00E5FF);
        dataSet.setCircleColor(0xFFFFFFFF);
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircles(true);
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(0xFFFFFFFF);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(true);
        dataSet.setFillAlpha(50);
        dataSet.setFillColor(0xFF00E5FF);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return formatPace(value);
            }
        });
        
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(0xFFFFFFFF);
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(Math.min(dates.size(), 5));
        
        if (entries.size() == 1) {
            xAxis.setAxisMinimum(-0.5f);
            xAxis.setAxisMaximum(0.5f);
        }

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(0xFFFFFFFF);
        leftAxis.setTextSize(10f);
        leftAxis.setInverted(false);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(0x33FFFFFF);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.animateX(1000);
        chartContainer.addView(lineChart, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, spToPx(250)));
    }

    private void addHeartRateTrendChart(List<WorkoutEntity> allWorkouts) {
        List<Entry> entries = new ArrayList<>();
        List<String> dates = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM", Locale.getDefault());
        List<WorkoutEntity> sorted = new ArrayList<>(allWorkouts);
        Collections.sort(sorted, (a, b) -> Long.compare(a.getScheduledDate(), b.getScheduledDate()));
        
        int count = 0;
        for (WorkoutEntity w : sorted) {
            if (w.isCompleted() && w.getAvgHeartRate() > 0) {
                entries.add(new Entry(count, (float) w.getAvgHeartRate()));
                dates.add(sdf.format(new java.util.Date(w.getScheduledDate())));
                count++;
            }
        }
        if (entries.isEmpty()) return;
        addChartTitle("HEART RATE TREND");
        
        LineChart lineChart = new LineChart(this);
        LineDataSet dataSet = new LineDataSet(entries, "Avg HR");
        
        android.content.SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
        int maxHR = 0;
        String maxStr = prefs.getString("userMaxHR", "");
        if (!maxStr.isEmpty()) {
            try { maxHR = Integer.parseInt(maxStr); } catch (Exception ignored) {}
        } else {
            String ageStr = prefs.getString("userAge", "30");
            try { maxHR = 220 - Integer.parseInt(ageStr); } catch (Exception ignored) {}
        }

        List<Integer> colors = new ArrayList<>();
        for (Entry e : entries) {
            float hr = e.getY();
            if (maxHR > 0) {
                double pct = hr / maxHR;
                if (pct >= 0.9) colors.add(0xFFFF3B30); // Zone 5
                else if (pct >= 0.8) colors.add(0xFFFF9500); // Zone 4
                else if (pct >= 0.7) colors.add(0xFFFFD60A); // Zone 3
                else if (pct >= 0.6) colors.add(0xFF34C759); // Zone 2
                else colors.add(0xFF8E8E93); // Zone 1
            } else {
                colors.add(0xFFFF3B30);
            }
        }
        dataSet.setColors(colors);
        dataSet.setCircleColors(colors);
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircles(true);
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(0xFFFFFFFF);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(0xFFFFFFFF);
        xAxis.setTextSize(10f);
        xAxis.setGranularity(1f);
        xAxis.setDrawGridLines(false);
        xAxis.setLabelCount(Math.min(dates.size(), 5));

        if (entries.size() == 1) {
            xAxis.setAxisMinimum(-0.5f);
            xAxis.setAxisMaximum(0.5f);
        }

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setTextColor(0xFFFFFFFF);
        leftAxis.setTextSize(10f);
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(0x33FFFFFF);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.animateX(1000);
        chartContainer.addView(lineChart, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, spToPx(250)));
    }

    private void addPieChart(double completedPercent, String label) {
        PieChart pieChart = new PieChart(this);
        List<PieEntry> entries = new ArrayList<>();
        entries.add(new PieEntry((float) completedPercent, "Done"));
        entries.add(new PieEntry((float) (100.0 - completedPercent), "To Go"));
        
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{0xFFCCFF00, 0xFF00E5FF}); 
        dataSet.setSliceSpace(4f);
        
        dataSet.setValueTextColor(0xFF121212); 
        dataSet.setValueTextSize(16f);
        dataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format(Locale.getDefault(), "%.0f%%", value);
            }
        });

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText(label + " (%)");
        pieChart.setCenterTextColor(0xFFFFFFFF);
        pieChart.setCenterTextSize(18f);
        pieChart.setCenterTextTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        pieChart.setHoleColor(android.graphics.Color.TRANSPARENT);
        pieChart.setHoleRadius(65f);
        
        Legend l = pieChart.getLegend();
        l.setEnabled(true);
        l.setTextColor(0xFFFFFFFF);
        l.setTextSize(12f);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        
        pieChart.animateY(1200);
        chartContainer.addView(pieChart, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, spToPx(380)));
    }

    private void addTypeBreakdownChart(int intervals, int steady, int longRun, int tempo, int strength) {
        if (intervals + steady + longRun + tempo + strength == 0) return;
        addChartTitle("WORKOUT TYPES COMPLETED");
        PieChart pieChart = new PieChart(this);
        List<PieEntry> entries = new ArrayList<>();
        if (intervals > 0) entries.add(new PieEntry(intervals, "Intervals"));
        if (steady > 0) entries.add(new PieEntry(steady, "Steady"));
        if (longRun > 0) entries.add(new PieEntry(longRun, "Long Run"));
        if (tempo > 0) entries.add(new PieEntry(tempo, "Tempo"));
        if (strength > 0) entries.add(new PieEntry(strength, "Strength"));
        
        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(new int[]{0xFFCCFF00, 0xFF00E5FF, 0xFFFFD60A, 0xFFAF52DE, 0xFF8E8E93}); 
        dataSet.setSliceSpace(3f);
        dataSet.setValueTextColor(0xFF121212); 
        dataSet.setValueTextSize(14f);
        dataSet.setValueTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        dataSet.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.valueOf((int) value);
            }
        });

        PieData pieData = new PieData(dataSet);
        pieChart.setData(pieData);
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.setHoleColor(android.graphics.Color.TRANSPARENT);
        pieChart.setHoleRadius(65f);
        
        Legend l = pieChart.getLegend();
        l.setEnabled(true);
        l.setTextColor(0xFFFFFFFF);
        l.setTextSize(12f);
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setWordWrapEnabled(true);

        pieChart.animateY(1200);
        chartContainer.addView(pieChart, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, spToPx(380)));
    }

    private void addChartTitle(String titleText) {
        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextSize(12f);
        title.setTextColor(0xFFFFFFFF);
        title.setPadding(spToPx(16), spToPx(40), spToPx(16), spToPx(12));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setGravity(android.view.Gravity.START);
        chartContainer.addView(title);
    }

    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }
}
