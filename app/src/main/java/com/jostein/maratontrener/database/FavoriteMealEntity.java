package com.jostein.maratontrener.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "favorite_meals")
public class FavoriteMealEntity {

    @PrimaryKey
    @NonNull
    private String mealId;

    public FavoriteMealEntity(@NonNull String mealId) {
        this.mealId = mealId;
    }

    @NonNull
    public String getMealId() {
        return mealId;
    }

    public void setMealId(@NonNull String mealId) {
        this.mealId = mealId;
    }
}
