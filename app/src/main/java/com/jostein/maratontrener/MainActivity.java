package com.jostein.maratontrener;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.TextView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.File;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private WorkoutAdapter adapter;
    private WorkoutDao workoutDao;
    private TextView headerText;
    private ShapeableImageView imageProfileHome;
    private FloatingActionButton fabAddWorkout;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        findViewById(R.id.btnCreatePlanMain).setOnClickListener(v -> {
            startActivity(new Intent(this, EditWorkoutActivity.class));
        });

        findViewById(R.id.btnBackMain).setOnClickListener(v -> finish());

        headerText = findViewById(R.id.headerText);
        imageProfileHome = findViewById(R.id.imageProfileHome);
        recyclerView = findViewById(R.id.recyclerView);
        fabAddWorkout = findViewById(R.id.fabAddWorkout);
        bottomNavigationView = findViewById(R.id.bottomNavigation);

        imageProfileHome.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ProfileActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkoutAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        workoutDao = WorkoutDatabase.getDatabase(getApplicationContext()).workoutDao();

        fabAddWorkout.setOnClickListener(view -> {
            startActivity(new Intent(MainActivity.this, EditWorkoutActivity.class));
        });

        setupNavigation();
    }

    private void setupNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_log);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_log) return true;
            
            Intent intent = null;
            if (id == R.id.nav_home) intent = new Intent(this, HomeActivity.class);
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

    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_manual) {
            startActivity(new Intent(this, EditWorkoutActivity.class));
            return true;
        } else if (id == R.id.action_add_sample_data) {
            addSampleData();
            return true;
        } else if (id == R.id.action_clear_all_data) {
            clearAllData();
            return true;
        } else if (id == R.id.action_view_progress) {
            startActivity(new Intent(this, ProgressActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void addSampleData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
            long eventDate = prefs.getLong("eventDate", System.currentTimeMillis() + (1000L * 60 * 60 * 24 * 90));
            List<WorkoutEntity> samples = WorkoutUtils.generateSampleWorkouts(eventDate);
            workoutDao.insertAll(samples);
            runOnUiThread(() -> {
                loadWorkouts();
                android.widget.Toast.makeText(this, "Sample data added", android.widget.Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void clearAllData() {
        Executors.newSingleThreadExecutor().execute(() -> {
            workoutDao.deleteAll();
            runOnUiThread(() -> {
                loadWorkouts();
                android.widget.Toast.makeText(this, "Training plan cleared", android.widget.Toast.LENGTH_SHORT).show();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.getMenu().findItem(R.id.nav_log).setChecked(true);
        }
        updateHeader();
        loadWorkouts();
        loadProfileImage();
    }

    private void loadProfileImage() {
        try {
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
            String path = prefs.getString("profileImagePath", null);
            
            if (path != null && new File(path).exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                Bitmap b = BitmapFactory.decodeFile(path, options);
                
                if (b != null) {
                    imageProfileHome.setImageBitmap(b);
                    imageProfileHome.setPadding(0, 0, 0, 0);
                    imageProfileHome.setImageTintList(null);
                    imageProfileHome.setColorFilter(null);
                }
            } else {
                imageProfileHome.setImageResource(R.drawable.ic_person);
                imageProfileHome.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
                imageProfileHome.setPadding(spToPx(4), spToPx(4), spToPx(4), spToPx(4)); // Consistent padding
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }

    private void updateHeader() {
        headerText.setText(R.string.training_plan_title);
    }

    private void loadWorkouts() {
        boolean filterMissed = getIntent().getBooleanExtra("FILTER_MISSED", false);
        
        Executors.newSingleThreadExecutor().execute(() -> {
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
            String currentPlan = prefs.getString("eventName", "");
            
            List<WorkoutEntity> results;
            if (filterMissed) {
                results = workoutDao.getMissedWorkoutsSync(System.currentTimeMillis());
            } else if (currentPlan.isEmpty()) {
                results = workoutDao.getAllWorkouts();
            } else {
                results = workoutDao.getWorkoutsByPlan(currentPlan);
                // Fallback: If the current plan has no workouts (e.g. name mismatch after edit), show everything
                if (results == null || results.isEmpty()) {
                    results = workoutDao.getAllWorkouts();
                }
            }

            final List<WorkoutEntity> finalWorkouts = results;
            runOnUiThread(() -> {
                if (finalWorkouts != null) {
                    if (filterMissed) {
                        headerText.setText("MISSED ACTIVITIES");
                    } else {
                        updateHeader();
                    }
                    adapter.setWorkoutList(finalWorkouts);
                    adapter.notifyDataSetChanged();
                }
            });
        });
    }
}
