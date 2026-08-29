package com.jostein.maratontrener.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import java.util.List;

@Dao
public interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY scheduledDate ASC")
    List<WorkoutEntity> getAllWorkouts();

    @Query("SELECT * FROM workouts ORDER BY scheduledDate ASC")
    List<WorkoutEntity> getAllWorkoutsSync();

    @Query("SELECT * FROM workouts WHERE id = :id")
    WorkoutEntity getWorkoutById(int id);

    @Query("SELECT * FROM workouts WHERE isCompleted = 1")
    List<WorkoutEntity> getCompletedWorkouts();

    @Query("SELECT * FROM workouts WHERE isCompleted = 1 ORDER BY scheduledDate DESC LIMIT 1")
    WorkoutEntity getLatestCompletedWorkout();

    @Query("SELECT * FROM workouts WHERE planName = :planName ORDER BY scheduledDate ASC")
    List<WorkoutEntity> getWorkoutsByPlan(String planName);

    @Query("SELECT * FROM workouts WHERE isCompleted = 0 AND scheduledDate < :now ORDER BY scheduledDate ASC")
    List<WorkoutEntity> getMissedWorkoutsSync(long now);

    @Query("SELECT DISTINCT planName FROM workouts")
    List<String> getAllPlanNames();

    @Insert
    void insertAll(List<WorkoutEntity> workouts);

    @Insert
    void insertWorkout(WorkoutEntity workout);

    @Update
    void updateWorkout(WorkoutEntity workout);

    @androidx.room.Delete
    void deleteWorkout(WorkoutEntity workout);

    @Query("DELETE FROM workouts WHERE planName = :planName")
    void deletePlan(String planName);

    @Query("DELETE FROM workouts WHERE isCompleted = 0")
    void deleteUncompletedWorkouts();

    @Query("DELETE FROM workouts")
    void deleteAll();

    @Query("SELECT COALESCE(SUM(distance), 0.0) FROM workouts WHERE shoeId = :shoeId AND isCompleted = 1")
    double getMileageForShoeSync(String shoeId);
}