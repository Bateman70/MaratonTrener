package com.jostein.maratontrener.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import java.util.List;

@Dao
public interface FavoriteMealDao {

    @Query("SELECT mealId FROM favorite_meals")
    List<String> getAllFavoriteMealIds();

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_meals WHERE mealId = :mealId LIMIT 1)")
    boolean isFavorite(String mealId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavorite(FavoriteMealEntity favoriteMeal);

    @Delete
    void deleteFavorite(FavoriteMealEntity favoriteMeal);

    @Query("DELETE FROM favorite_meals WHERE mealId = :mealId")
    void deleteFavoriteById(String mealId);
}
