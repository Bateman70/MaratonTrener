package com.jostein.maratontrener;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import java.util.List;
import java.util.concurrent.Executors;

public class ArchiveActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ArchiveAdapter adapter;
    private WorkoutDao workoutDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_race_archive);

        Toolbar toolbar = findViewById(R.id.toolbarArchive);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        findViewById(R.id.btnBackArchive).setOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerArchive);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        workoutDao = WorkoutDatabase.getDatabase(this).workoutDao();
        loadArchive();
    }

    private void loadArchive() {
        Executors.newSingleThreadExecutor().execute(() -> {
            List<String> plans = workoutDao.getAllPlanNames();
            runOnUiThread(() -> {
                adapter = new ArchiveAdapter(plans, planName -> {
                    // Switch current active plan in preferences and go to main
                    SharedPreferences prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE);
                    prefs.edit().putString("eventName", planName).apply();
                    
                    Intent intent = new Intent(ArchiveActivity.this, HomeActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });
                recyclerView.setAdapter(adapter);
            });
        });
    }
}