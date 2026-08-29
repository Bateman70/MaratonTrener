package com.jostein.maratontrener.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shoes")
public class ShoeEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private double initialMileage;
    private double mileageLimit;
    private boolean isRetired;

    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getInitialMileage() {
        return initialMileage;
    }

    public void setInitialMileage(double initialMileage) {
        this.initialMileage = initialMileage;
    }

    public double getMileageLimit() {
        return mileageLimit;
    }

    public void setMileageLimit(double mileageLimit) {
        this.mileageLimit = mileageLimit;
    }

    public boolean isRetired() {
        return isRetired;
    }

    public void setRetired(boolean retired) {
        isRetired = retired;
    }
}
