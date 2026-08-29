package com.jostein.maratontrener;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

public class LogFragment extends Fragment {

    private RecyclerView recyclerView;
    private WorkoutAdapter adapter;
    private WorkoutDao workoutDao;
    private TextView headerText;
    private ShapeableImageView imageProfileHome;
    private FloatingActionButton fabAddWorkout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_log, container, false);

        view.findViewById(R.id.btnCreatePlanMain).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditWorkoutActivity.class));
        });

        headerText = view.findViewById(R.id.headerText);
        imageProfileHome = view.findViewById(R.id.imageProfileHome);
        recyclerView = view.findViewById(R.id.recyclerView);
        fabAddWorkout = view.findViewById(R.id.fabAddWorkout);

        imageProfileHome.setOnClickListener(v -> {
            if (getActivity() instanceof MainContainerActivity) {
                ((MainContainerActivity) getActivity()).switchToTab(R.id.nav_profile);
            }
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new WorkoutAdapter(requireContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        workoutDao = WorkoutDatabase.getDatabase(requireContext()).workoutDao();

        fabAddWorkout.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), EditWorkoutActivity.class));
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWorkouts();
        loadProfileImage();
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
                    imageProfileHome.setImageBitmap(b);
                    imageProfileHome.setPadding(0, 0, 0, 0);
                    imageProfileHome.setImageTintList(null);
                    imageProfileHome.setColorFilter(null);
                }
            } else {
                imageProfileHome.setImageResource(R.drawable.ic_person);
                imageProfileHome.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
                imageProfileHome.setPadding(spToPx(4), spToPx(4), spToPx(4), spToPx(4));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }

    private void loadWorkouts() {
        Executors.newSingleThreadExecutor().execute(() -> {
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext());
            String currentPlan = prefs.getString("eventName", "");
            List<WorkoutEntity> results;
            if (currentPlan.isEmpty()) results = workoutDao.getAllWorkouts();
            else {
                results = workoutDao.getWorkoutsByPlan(currentPlan);
                if (results == null || results.isEmpty()) results = workoutDao.getAllWorkouts();
            }
            final List<WorkoutEntity> finalWorkouts = results;
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (finalWorkouts != null) {
                    adapter.setWorkoutList(finalWorkouts);
                    adapter.notifyDataSetChanged();
                }
            });
        });
    }
}