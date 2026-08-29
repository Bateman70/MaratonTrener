package com.jostein.maratontrener.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ShoeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ShoeEntity shoe);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ShoeEntity> shoes);

    @Update
    void update(ShoeEntity shoe);

    @Delete
    void delete(ShoeEntity shoe);

    @Query("SELECT * FROM shoes ORDER BY name ASC")
    List<ShoeEntity> getAllShoesSync();

    @Query("SELECT * FROM shoes WHERE isRetired = 0 ORDER BY name ASC")
    List<ShoeEntity> getActiveShoesSync();

    @Query("SELECT * FROM shoes WHERE id = :id")
    ShoeEntity getShoeByIdSync(String id);

    @Query("DELETE FROM shoes")
    void deleteAllShoes();
}
