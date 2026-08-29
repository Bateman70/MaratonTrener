package com.jostein.maratontrener.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

// Version 10 adding Running Shoes Tracking
@Database(
    entities = {WorkoutEntity.class, FavoriteMealEntity.class, ShoeEntity.class}, 
    version = 10, 
    exportSchema = true
)
public abstract class WorkoutDatabase extends RoomDatabase {
    private static volatile WorkoutDatabase INSTANCE;

    public abstract WorkoutDao workoutDao();
    public abstract FavoriteMealDao favoriteMealDao();
    public abstract ShoeDao shoeDao();

    static final Migration MIGRATION_9_10 = new Migration(9, 10) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE workouts ADD COLUMN shoeId TEXT");
            database.execSQL("CREATE TABLE IF NOT EXISTS `shoes` (" +
                    "`id` TEXT NOT NULL, " +
                    "`name` TEXT, " +
                    "`initialMileage` REAL NOT NULL, " +
                    "`mileageLimit` REAL NOT NULL, " +
                    "`isRetired` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))");
        }
    };

    public static WorkoutDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (WorkoutDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    WorkoutDatabase.class, "workout_database")
                            .addMigrations(MIGRATION_9_10)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}