package com.jostein.maratontrener;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class WorkoutAdapter extends RecyclerView.Adapter<WorkoutAdapter.WorkoutViewHolder> {

    private final Context context;
    private List<WorkoutEntity> workoutList;

    public WorkoutAdapter(Context context, List<WorkoutEntity> workoutList) {
        this.context = context;
        this.workoutList = workoutList;
    }

    public void setWorkoutList(List<WorkoutEntity> workoutList) {
        this.workoutList = workoutList;
    }

    @NonNull
    @Override
    public WorkoutViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_workout, parent, false);
        return new WorkoutViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WorkoutViewHolder holder, int position) {
        WorkoutEntity workout = workoutList.get(position);

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault());
        holder.dateText.setText(sdf.format(new Date(workout.getScheduledDate())));
        holder.typeText.setText(WorkoutUtils.getWorkoutTypeWithIcon(workout.getWorkoutType()));

        String details;
        if ("STRENGTH & CORE".equalsIgnoreCase(workout.getWorkoutType())) {
            details = "";
            holder.descriptionText.setText(workout.getDescription());
            holder.descriptionText.setVisibility(View.VISIBLE);
        } else if ("INTERVALS".equalsIgnoreCase(workout.getWorkoutType())) {
            if (workout.getIntervalCount() > 0) {
                details = String.format(Locale.getDefault(), "%dx %s at %s pace",
                        workout.getIntervalCount(), workout.getIntervalValue(), workout.getIntervalPace());
            } else {
                details = String.format(Locale.getDefault(), "%.1f km", workout.getDistance());
            }
            holder.descriptionText.setText(workout.getDescription());
            holder.descriptionText.setVisibility(View.VISIBLE);
        } else {
            double distance = workout.getDistance();
            double pace = workout.getPace();
            
            if (pace > 0) {
                int min = (int) pace;
                int sec = (int) Math.round((pace - min) * 60);
                double speed = 60.0 / pace;
                details = String.format(Locale.getDefault(), "%.1f km | Pace: %02d:%02d | Speed: %.1f km/h",
                        distance, min, sec, speed);
            } else {
                details = String.format(Locale.getDefault(), "%.1f km", distance);
            }

            if (workout.isCompleted() && workout.getAvgHeartRate() > 0) {
                details += String.format(Locale.getDefault(), " | HR: %d", workout.getAvgHeartRate());
            }
            
            if (workout.getDescription() != null && !workout.getDescription().isEmpty()) {
                holder.descriptionText.setText(workout.getDescription());
                holder.descriptionText.setVisibility(View.VISIBLE);
            } else {
                holder.descriptionText.setVisibility(View.GONE);
            }
        }
        holder.detailsText.setText(details);

        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
        cal.set(java.util.Calendar.MINUTE, 0);
        cal.set(java.util.Calendar.SECOND, 0);
        cal.set(java.util.Calendar.MILLISECOND, 0);
        long todayStart = cal.getTimeInMillis();

        // Visual feedback for missed vs completed
        if (workout.isCompleted()) {
            holder.typeText.setTextColor(context.getResources().getColor(R.color.success_green));
            holder.dateText.setTextColor(context.getResources().getColor(R.color.text_secondary));
        } else if (workout.getScheduledDate() < todayStart) {
            holder.typeText.setTextColor(context.getResources().getColor(R.color.error_red));
            holder.dateText.setTextColor(context.getResources().getColor(R.color.error_red));
            holder.dateText.setAlpha(0.7f);
        } else {
            holder.typeText.setTextColor(context.getResources().getColor(R.color.electric_lime));
            holder.dateText.setTextColor(context.getResources().getColor(R.color.text_secondary));
        }

        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(workout.isCompleted());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            workout.setCompleted(isChecked);
            Executors.newSingleThreadExecutor().execute(() -> {
                WorkoutDatabase.getDatabase(context).workoutDao().updateWorkout(workout);
            });
            // Refresh colors on check change
            notifyItemChanged(position);
        });

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, EditWorkoutActivity.class);
            intent.putExtra("WORKOUT_ID", workout.getId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return workoutList != null ? workoutList.size() : 0;
    }

    public static class WorkoutViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, typeText, detailsText, descriptionText;
        CheckBox checkBox;

        public WorkoutViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.textDate);
            typeText = itemView.findViewById(R.id.textType);
            detailsText = itemView.findViewById(R.id.textDetails);
            descriptionText = itemView.findViewById(R.id.textDescription);
            checkBox = itemView.findViewById(R.id.completedCheckbox);
        }
    }
}