package com.jostein.maratontrener.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workouts")
public class WorkoutEntity {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private long scheduledDate;
    private double distance;
    private double pace;
    private String workoutType;
    private String description;
    private String notes;
    private int weekNumber;
    private boolean isCompleted;
    private String planName;
    private double totalDuration; // in minutes
    private int maxHeartRate;
    private int avgHeartRate;

    // Interval Specifics
    private int intervalCount;
    private String intervalValue; // e.g., "800m" or "4 min"
    private String intervalPace;  // e.g., "4:15" or "14 km/h"

    private String shoeId;

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getIntervalCount() { return intervalCount; }
    public void setIntervalCount(int intervalCount) { this.intervalCount = intervalCount; }

    public String getIntervalValue() { return intervalValue; }
    public void setIntervalValue(String intervalValue) { this.intervalValue = intervalValue; }

    public String getIntervalPace() { return intervalPace; }
    public void setIntervalPace(String intervalPace) { this.intervalPace = intervalPace; }

    public int getMaxHeartRate() { return maxHeartRate; }
    public void setMaxHeartRate(int maxHeartRate) { this.maxHeartRate = maxHeartRate; }

    public int getAvgHeartRate() { return avgHeartRate; }
    public void setAvgHeartRate(int avgHeartRate) { this.avgHeartRate = avgHeartRate; }

    public double getTotalDuration() { return totalDuration; }
    public void setTotalDuration(double totalDuration) { this.totalDuration = totalDuration; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public long getScheduledDate() { return scheduledDate; }
    public void setScheduledDate(long scheduledDate) { this.scheduledDate = scheduledDate; }

    public double getDistance() { return distance; }
    public void setDistance(double distance) { this.distance = distance; }

    public double getPace() { return pace; }
    public void setPace(double pace) { this.pace = pace; }

    public String getWorkoutType() { return workoutType; }
    public void setWorkoutType(String workoutType) { this.workoutType = workoutType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getWeekNumber() { return weekNumber; }
    public void setWeekNumber(int weekNumber) { this.weekNumber = weekNumber; }

    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }

    public String getShoeId() { return shoeId; }
    public void setShoeId(String shoeId) { this.shoeId = shoeId; }
}