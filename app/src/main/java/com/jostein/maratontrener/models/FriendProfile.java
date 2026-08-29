package com.jostein.maratontrener.models;

import java.util.Map;

public class FriendProfile {
    public String id;
    public String name;
    public double distance;
    public long consistency;
    public long workoutsDone;
    public long workoutsTotal;
    public long lastUpdate;
    public String currentRace; // Added for live race name support
    public Map<String, String> highFives;

    public FriendProfile() {
        // Required for Firebase
    }

    public int getHighFiveCount() {
        return highFives != null ? highFives.size() : 0;
    }

    public boolean hasHighFiveFrom(String senderId) {
        return highFives != null && highFives.containsKey(senderId);
    }
}