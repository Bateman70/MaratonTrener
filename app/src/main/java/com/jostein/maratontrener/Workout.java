package com.jostein.maratontrener;

import java.util.Date;

public class Workout {

    private int weekNumber;
    private String workoutType;
    private String description;
    private double plannedDistance;
    private String plannedPace;
    private Date scheduledDate;

    public int getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(int weekNumber) {
        this.weekNumber = weekNumber;
    }

    public String getWorkoutType() {
        return workoutType;
    }

    public void setWorkoutType(String workoutType) {
        this.workoutType = workoutType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getPlannedDistance() {
        return plannedDistance;
    }

    public void setPlannedDistance(double plannedDistance) {
        this.plannedDistance = plannedDistance;
    }

    public String getPlannedPace() {
        return plannedPace;
    }

    public void setPlannedPace(String plannedPace) {
        this.plannedPace = plannedPace;
    }

    public Date getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(Date scheduledDate) {
        this.scheduledDate = scheduledDate;
    }
}
