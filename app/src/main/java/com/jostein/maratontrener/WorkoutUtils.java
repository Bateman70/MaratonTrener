package com.jostein.maratontrener;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;
import com.jostein.maratontrener.database.ShoeDao;
import com.jostein.maratontrener.database.ShoeEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

public class WorkoutUtils {

    public static List<WorkoutEntity> generateSampleWorkouts(long eventDateMillis) {
        List<WorkoutEntity> workouts = new ArrayList<>();
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(eventDateMillis);

        for (int i = 0; i < 12; i++) {
            for (int j = 0; j < 4; j++) {
                WorkoutEntity workout = new WorkoutEntity();
                workout.setWeekNumber(i + 1);

                if (j == 0) {
                    workout.setWorkoutType("INTERVALS");
                    workout.setDescription("High-intensity intervals to improve speed and endurance.");
                    workout.setDistance(6 + i * 0.5);
                    workout.setPace(4.5);
                } else if (j == 1) {
                    workout.setWorkoutType("STEADY RUN");
                    workout.setDescription("Maintain a steady pace just below race pace.");
                    workout.setDistance(8 + i * 0.6);
                    workout.setPace(5.0);
                } else if (j == 2) {
                    workout.setWorkoutType("STRENGTH & CORE");
                    int wk = i + 1;
                    if (wk <= 4) {
                        workout.setDescription("Strength Phase 1 (Foundation): Glute Bridges (3x15), Squats (3x15), Plank (3x60s), Supermans (3x12), Bird-Dogs (3x12), Push-ups (3x10), Walking Lunges (3x10/leg). Focus on posture and form.");
                    } else if (wk <= 8) {
                        workout.setDescription("Strength Phase 2 (Strength & Back Focus): Dumbbell Rows (3x10/arm), Single-Leg Deadlifts (3x10/leg), Tricep Dips (3x12), Step-ups (3x12/leg), Side Plank (3x30s/side), Leg Raises (3x12). Target back fatigue.");
                    } else {
                        workout.setDescription("Strength Phase 3 (Peak Power & Posture): Dumbbell Rows (3x12/arm), Supermans (3x15), Single-Leg Glute Bridges (3x10/leg), Plank w/ Shoulder Taps (3x45s), Lunges w/ Twist (3x10/leg), Single-Leg Deadlifts (3x12/leg). Shock absorption for asphalt.");
                    }
                    workout.setDistance(0);
                    workout.setPace(0);
                } else {
                    workout.setWorkoutType("LONG RUN");
                    workout.setDescription("Easy-paced long distance run.");
                    workout.setDistance(10 + i * 2);
                    workout.setPace(6.0);
                }

                Calendar workoutDate = (Calendar) calendar.clone();
                workoutDate.add(Calendar.DAY_OF_YEAR, -((11 - i) * 7 + j * 2));
                workout.setScheduledDate(workoutDate.getTimeInMillis());
                workout.setCompleted(workoutDate.getTimeInMillis() < System.currentTimeMillis());
                workout.setPlanName("Sample Plan");

                workouts.add(workout);
            }
        }
        return workouts;
    }

    public static String getWorkoutTypeWithIcon(String type) {
        if (type == null) return "";
        String upper = type.toUpperCase().trim();
        if (upper.contains("INTERVAL")) {
            return "⚡ " + type;
        } else if (upper.contains("LONG RUN")) {
            return "🏃‍♂️ " + type;
        } else if (upper.contains("TEMPO")) {
            return "🔥 " + type;
        } else if (upper.contains("STEADY") || upper.contains("EASY")) {
            return "🕊️ " + type;
        } else if (upper.contains("RECOVERY")) {
            return "🔋 " + type;
        } else if (upper.contains("STRENGTH") || upper.contains("CORE")) {
            return "💪 " + type;
        } else if (upper.contains("WALK")) {
            return "🚶‍♂️ " + type;
        } else if (upper.contains("REST")) {
            return "🛋️ " + type;
        }
        return type;
    }

    public static void uploadWorkoutsToFirebase(Context context) {
        try {
            SharedPreferences buddyPrefs = SecurityUtils.getEncryptedPrefs(context, "BuddyPrefs");
            String myId = buddyPrefs.getString("my_id", "CH020721");
            if (myId == null) return;

            SharedPreferences userPrefs = SecurityUtils.getEncryptedPrefs(context);
            DatabaseReference profileRef = FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/")
                    .getReference("profiles")
                    .child(myId);

            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    // Check cloud timestamp first to decide whether to download or upload
                    com.google.android.gms.tasks.Task<com.google.firebase.database.DataSnapshot> task = profileRef.get();
                    com.google.firebase.database.DataSnapshot snapshot = com.google.android.gms.tasks.Tasks.await(task);
                    
                    boolean shouldDownload = false;
                    if (snapshot.exists()) {
                        Long cloudLastUpdate = snapshot.child("lastUpdate").getValue(Long.class);
                        long localLastUpdate = userPrefs.getLong("profileLastUpdate", 0);
                        if (cloudLastUpdate != null && cloudLastUpdate > localLastUpdate) {
                            shouldDownload = true;
                        }
                    }

                    if (shouldDownload) {
                        // Cloud is newer. Pull workouts and profile from cloud.
                        android.util.Log.d("Sync", "Cloud is newer. Pulling profile and workouts...");
                        pullProfileFromSnapshot(context, snapshot, userPrefs, buddyPrefs);
                        downloadWorkoutsFromCloud(context, myId);
                    } else {
                        // Local is newer or equal. Upload workouts and then upload profile stats.
                        android.util.Log.d("Sync", "Local is newer. Uploading workouts...");
                        uploadLocalWorkouts(context, myId);
                    }
                } catch (Exception e) {
                    android.util.Log.e("Sync", "Error in background sync: " + e.getMessage());
                }
            });
        } catch (Exception ignored) {}
    }

    private static void pullProfileFromSnapshot(Context context, com.google.firebase.database.DataSnapshot snapshot, SharedPreferences userPrefs, SharedPreferences buddyPrefs) {
        try {
            SharedPreferences.Editor editor = userPrefs.edit();
            String cName = snapshot.child("name").getValue(String.class);
            if (cName != null && !cName.isEmpty()) editor.putString("userName", cName);
            
            String cNickname = snapshot.child("nickname").getValue(String.class);
            if (cNickname == null || cNickname.isEmpty()) cNickname = cName;
            if (cNickname != null && !cNickname.isEmpty()) {
                editor.putString("userNickname", cNickname);
                buddyPrefs.edit().putString("my_name", cNickname).apply();
            }
            
            String cEmail = snapshot.child("email").getValue(String.class);
            if (cEmail != null) editor.putString("userEmail", cEmail);
            
            String cLocation = snapshot.child("eventLocation").getValue(String.class);
            if (cLocation != null) editor.putString("eventLocation", cLocation);
            
            Object ageVal = snapshot.child("age").getValue();
            if (ageVal != null) editor.putString("userAge", String.valueOf(ageVal));
            
            Object weightVal = snapshot.child("weight").getValue();
            if (weightVal != null) editor.putString("userWeight", String.valueOf(weightVal));
            
            Object maxHrVal = snapshot.child("maxHr").getValue();
            if (maxHrVal != null) editor.putString("userMaxHR", String.valueOf(maxHrVal));
            
            String pb10k = snapshot.child("pb10k").getValue(String.class);
            if (pb10k != null) editor.putString("pb10k", pb10k);
            
            String pbHalf = snapshot.child("pbHalf").getValue(String.class);
            if (pbHalf != null) editor.putString("pbHalf", pbHalf);
            
            String pbFull = snapshot.child("pbFull").getValue(String.class);
            if (pbFull != null) editor.putString("pbFull", pbFull);
            
            Object planStartVal = snapshot.child("planStartDate").getValue();
            if (planStartVal != null) {
                try {
                    editor.putLong("planStartDate", Long.parseLong(String.valueOf(planStartVal)));
                } catch (Exception ignored) {}
            }

            String avatarBase64 = snapshot.child("avatar").getValue(String.class);
            if (avatarBase64 != null && avatarBase64.startsWith("data:image")) {
                try {
                    int commaIdx = avatarBase64.indexOf(",");
                    if (commaIdx != -1) {
                        String cleanBase64 = avatarBase64.substring(commaIdx + 1);
                        byte[] decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT);
                        java.io.File dir = new java.io.File(context.getFilesDir(), "profile_images");
                        if (!dir.exists()) dir.mkdirs();
                        java.io.File file = new java.io.File(dir, "profile.jpg");
                        java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                        fos.write(decodedBytes);
                        fos.close();
                        editor.putString("profileImagePath", file.getAbsolutePath());
                    }
                } catch (Exception ignored) {}
            }
            
            restoreGpxFromSnapshot(editor, snapshot);
            
            Long cloudLastUpdate = snapshot.child("lastUpdate").getValue(Long.class);
            if (cloudLastUpdate != null) {
                editor.putLong("profileLastUpdate", cloudLastUpdate);
            }
            editor.apply();
        } catch (Exception ignored) {}
    }

    private static void downloadWorkoutsFromCloud(Context context, String myId) {
        try {
            DatabaseReference workoutsRef = FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/")
                    .getReference("workouts")
                    .child(myId);
            com.google.android.gms.tasks.Task<com.google.firebase.database.DataSnapshot> task = workoutsRef.get();
            com.google.firebase.database.DataSnapshot snapshot = com.google.android.gms.tasks.Tasks.await(task);
            
            if (snapshot.exists()) {
                List<WorkoutEntity> workoutsToInsert = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                    try {
                        String dateStr = child.child("scheduledDate").getValue(String.class);
                        long dateMs = System.currentTimeMillis();
                        if (dateStr != null) {
                            java.util.Date parsedDate = sdf.parse(dateStr);
                            if (parsedDate != null) dateMs = parsedDate.getTime();
                        }
                        WorkoutEntity w = new WorkoutEntity();
                        w.setScheduledDate(dateMs);
                        
                        Integer weekNumber = child.child("weekNumber").getValue(Integer.class);
                        w.setWeekNumber(weekNumber != null ? weekNumber : 1);
                        w.setWorkoutType(child.child("workoutType").getValue(String.class));
                        w.setPlanName(child.child("planName").getValue(String.class));
                        
                        Double distance = child.child("distance").getValue(Double.class);
                        w.setDistance(distance != null ? distance : 0.0);
                        
                        Double pace = child.child("pace").getValue(Double.class);
                        w.setPace(pace != null ? pace : 0.0);
                        
                        Double duration = child.child("totalDuration").getValue(Double.class);
                        w.setTotalDuration(duration != null ? duration : 0.0);
                        
                        Integer avgHr = child.child("avgHeartRate").getValue(Integer.class);
                        w.setAvgHeartRate(avgHr != null ? avgHr : 0);
                        
                        w.setDescription(child.child("description").getValue(String.class));
                        w.setNotes(child.child("notes").getValue(String.class));
                        
                        Boolean isCompleted = child.child("isCompleted").getValue(Boolean.class);
                        w.setCompleted(isCompleted != null ? isCompleted : false);
                        
                        Integer intervalCount = child.child("intervalCount").getValue(Integer.class);
                        w.setIntervalCount(intervalCount != null ? intervalCount : 0);
                        w.setIntervalValue(child.child("intervalValue").getValue(String.class));
                        w.setIntervalPace(child.child("intervalPace").getValue(String.class));
                        
                        Integer maxHr = child.child("maxHeartRate").getValue(Integer.class);
                        w.setMaxHeartRate(maxHr != null ? maxHr : 0);
                        w.setShoeId(child.child("shoeId").getValue(String.class));
                        
                        workoutsToInsert.add(w);
                    } catch (Exception ignored) {}
                }
                
                if (!workoutsToInsert.isEmpty()) {
                    WorkoutDao dao = WorkoutDatabase.getDatabase(context).workoutDao();
                    dao.deleteAll();
                    dao.insertAll(workoutsToInsert);
                }
            }
        } catch (Exception ignored) {}
    }

    private static void uploadLocalWorkouts(Context context, String myId) {
        try {
            WorkoutDao workoutDao = WorkoutDatabase.getDatabase(context).workoutDao();
            List<WorkoutEntity> workouts = workoutDao.getAllWorkoutsSync();
            DatabaseReference workoutsRef = FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/")
                    .getReference("workouts")
                    .child(myId);
            Map<String, Object> workoutsMap = new HashMap<>();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            for (WorkoutEntity w : workouts) {
                Map<String, Object> wMap = new HashMap<>();
                wMap.put("scheduledDate", sdf.format(new java.util.Date(w.getScheduledDate())));
                wMap.put("weekNumber", w.getWeekNumber());
                wMap.put("workoutType", w.getWorkoutType());
                wMap.put("planName", w.getPlanName());
                wMap.put("distance", w.getDistance());
                wMap.put("pace", w.getPace());
                wMap.put("totalDuration", w.getTotalDuration());
                wMap.put("avgHeartRate", w.getAvgHeartRate());
                wMap.put("description", w.getDescription());
                wMap.put("notes", w.getNotes());
                wMap.put("isCompleted", w.isCompleted());
                wMap.put("intervalCount", w.getIntervalCount());
                wMap.put("intervalValue", w.getIntervalValue());
                wMap.put("intervalPace", w.getIntervalPace());
                wMap.put("maxHeartRate", w.getMaxHeartRate());
                wMap.put("shoeId", w.getShoeId() != null ? w.getShoeId() : "");
                workoutsMap.put("workout_" + w.getId(), wMap);
            }
            workoutsRef.setValue(workoutsMap);

            syncShoesWithFirebase(context, myId);
            uploadProfileStatsToFirebase(context, myId, workouts);
        } catch (Exception ignored) {}
    }

    private static void uploadProfileStatsToFirebase(Context context, String myId, List<WorkoutEntity> workouts) {
        try {
            SharedPreferences buddyPrefs = SecurityUtils.getEncryptedPrefs(context, "BuddyPrefs");
            SharedPreferences userPrefs = SecurityUtils.getEncryptedPrefs(context);

            DatabaseReference profileRef = FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/")
                    .getReference("profiles")
                    .child(myId);

            // Sync check: if cloud profile is newer, download it instead of uploading
            try {
                com.google.android.gms.tasks.Task<com.google.firebase.database.DataSnapshot> task = profileRef.get();
                com.google.firebase.database.DataSnapshot snapshot = com.google.android.gms.tasks.Tasks.await(task);
                if (snapshot.exists()) {
                    Long cloudLastUpdate = snapshot.child("lastUpdate").getValue(Long.class);
                    long localLastUpdate = userPrefs.getLong("profileLastUpdate", 0);
                    if (cloudLastUpdate != null && cloudLastUpdate > localLastUpdate) {
                        pullProfileFromSnapshot(context, snapshot, userPrefs, buddyPrefs);
                        return; // Stop and do not overwrite the cloud with old local details!
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("WorkoutUtils", "Error checking profile sync", e);
            }

            String name = buddyPrefs.getString("my_name", "Runner");

            int completed = 0; double dist = 0;
            long now = System.currentTimeMillis();
            long todayStart = (now / 86400000) * 86400000;
            int shouldBeDone = 0;

            java.util.Set<String> completedDays = new java.util.HashSet<>();
            SimpleDateFormat dayFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            for (WorkoutEntity w : workouts) {
                if (w.isCompleted()) {
                    completedDays.add(dayFormat.format(new java.util.Date(w.getScheduledDate())));
                }
            }

            for (WorkoutEntity w : workouts) {
                if (w.isCompleted()) {
                    completed++;
                    dist += w.getDistance();
                    if (w.getScheduledDate() < todayStart + 86400000) {
                        shouldBeDone++;
                    }
                } else {
                    if (w.getScheduledDate() < todayStart) {
                        String workoutDay = dayFormat.format(new java.util.Date(w.getScheduledDate()));
                        if (!completedDays.contains(workoutDay)) {
                            shouldBeDone++;
                        }
                    }
                }
            }
            int consistency = (shouldBeDone == 0) ? 0 : (completed * 100 / shouldBeDone);

            String currentPlan = userPrefs.getString("eventName", "Unknown Race");
            String eventType = userPrefs.getString("eventType", "");
            long eventDate = userPrefs.getLong("eventDate", 0);
            StringBuilder sb = new StringBuilder(currentPlan);
            if (!eventType.isEmpty()) sb.append(" - ").append(eventType);
            if (eventDate > 0) {
                SimpleDateFormat raceSdf = new SimpleDateFormat("d MMM yyyy", Locale.US);
                sb.append(": ").append(raceSdf.format(new java.util.Date(eventDate)));
            }

            Map<String, Object> profile = new HashMap<>();
            profile.put("name", userPrefs.getString("userName", "Runner"));
            profile.put("nickname", userPrefs.getString("userNickname", "Athlete"));
            profile.put("email", userPrefs.getString("userEmail", "athlete@example.com"));
            profile.put("eventLocation", userPrefs.getString("eventLocation", ""));
            profile.put("distance", dist);
            profile.put("consistency", consistency);
            profile.put("workoutsDone", completed);
            profile.put("workoutsTotal", workouts.size());
            profile.put("currentRace", sb.toString());
            profile.put("lastUpdate", now);

            // Add profile sync parameters
            profile.put("weight", userPrefs.getString("userWeight", ""));
            profile.put("age", userPrefs.getString("userAge", ""));
            profile.put("maxHr", userPrefs.getString("userMaxHR", ""));
            profile.put("pb10k", userPrefs.getString("pb10k", ""));
            profile.put("pbHalf", userPrefs.getString("pbHalf", ""));
            profile.put("pbFull", userPrefs.getString("pbFull", ""));
            profile.put("planStartDate", userPrefs.getLong("planStartDate", 0));

            // Add GPX route data if it exists
            String gpxPoints = userPrefs.getString("gpxRoutePoints", null);
            if (gpxPoints != null && !gpxPoints.isEmpty()) {
                Map<String, Object> gpxMap = new HashMap<>();
                gpxMap.put("name", userPrefs.getString("gpxRouteName", "Route"));
                gpxMap.put("distance", Double.longBitsToDouble(userPrefs.getLong("gpxDistance", Double.doubleToRawLongBits(0.0))));
                gpxMap.put("elevationGain", Double.longBitsToDouble(userPrefs.getLong("gpxElevationGain", Double.doubleToRawLongBits(0.0))));
                gpxMap.put("avgSlope", Double.longBitsToDouble(userPrefs.getLong("gpxAvgSlope", Double.doubleToRawLongBits(0.0))));
                
                try {
                    List<List<Double>> pointsList = new ArrayList<>();
                    String clean = gpxPoints.substring(2, gpxPoints.length() - 2); // remove outer [[ and ]]
                    String[] groups = clean.split("\\],\\s*\\[");
                    for (String grp : groups) {
                        String[] vals = grp.split(",");
                        List<Double> pt = new ArrayList<>();
                        for (String v : vals) {
                            pt.add(Double.parseDouble(v.trim()));
                        }
                        pointsList.add(pt);
                    }
                    gpxMap.put("points", pointsList);
                } catch (Exception ignored) {}
                profile.put("gpxRoute", gpxMap);
            } else {
                profile.put("gpxRoute", null);
            }

            // Read local profile image and convert to Base64 avatar
            String avatarBase64 = null;
            String imagePath = userPrefs.getString("profileImagePath", null);
            if (imagePath != null) {
                java.io.File file = new java.io.File(imagePath);
                if (file.exists()) {
                    try {
                        java.io.FileInputStream fis = new java.io.FileInputStream(file);
                        byte[] bytes = new byte[(int) file.length()];
                        fis.read(bytes);
                        fis.close();
                        avatarBase64 = "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP);
                    } catch (Exception ignored) {}
                }
            }
            if (avatarBase64 != null) {
                profile.put("avatar", avatarBase64);
            }
            
            List<String> favoriteMeals = new ArrayList<>();
            try {
                List<String> favs = WorkoutDatabase.getDatabase(context).favoriteMealDao().getAllFavoriteMealIds();
                if (favs != null) {
                    favoriteMeals.addAll(favs);
                }
            } catch (Exception ignored) {}
            profile.put("favoriteMeals", favoriteMeals);

            FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/")
                    .getReference("profiles")
                    .child(myId)
                    .setValue(profile);

            // Save the new lastUpdate timestamp locally so we are in sync
            userPrefs.edit().putLong("profileLastUpdate", now).apply();
        } catch (Exception ignored) {}
    }

    public static void restoreGpxFromSnapshot(SharedPreferences.Editor editor, com.google.firebase.database.DataSnapshot snapshot) {
        com.google.firebase.database.DataSnapshot gpxSnap = snapshot.child("gpxRoute");
        if (gpxSnap.exists()) {
            String gpxName = gpxSnap.child("name").getValue(String.class);
            Double gpxDist = gpxSnap.child("distance").getValue(Double.class);
            Double gpxGain = gpxSnap.child("elevationGain").getValue(Double.class);
            Double gpxSlope = gpxSnap.child("avgSlope").getValue(Double.class);
            
            StringBuilder sbPoints = new StringBuilder();
            sbPoints.append("[");
            com.google.firebase.database.DataSnapshot ptsSnap = gpxSnap.child("points");
            if (ptsSnap.exists()) {
                int count = 0;
                for (com.google.firebase.database.DataSnapshot ptSnap : ptsSnap.getChildren()) {
                    List<Double> coords = (List<Double>) ptSnap.getValue();
                    if (coords != null && coords.size() >= 4) {
                        if (count > 0) sbPoints.append(",");
                        sbPoints.append(String.format(java.util.Locale.US, "[%.6f,%.6f,%.1f,%.3f]", coords.get(0), coords.get(1), coords.get(2), coords.get(3)));
                        count++;
                    }
                }
            }
            sbPoints.append("]");
            
            editor.putString("gpxRoutePoints", sbPoints.toString());
            editor.putString("gpxRouteName", gpxName != null ? gpxName : "Route");
            editor.putLong("gpxDistance", Double.doubleToRawLongBits(gpxDist != null ? gpxDist : 0.0));
            editor.putLong("gpxElevationGain", Double.doubleToRawLongBits(gpxGain != null ? gpxGain : 0.0));
            editor.putLong("gpxAvgSlope", Double.doubleToRawLongBits(gpxSlope != null ? gpxSlope : 0.0));
        } else {
            editor.remove("gpxRoutePoints");
            editor.remove("gpxRouteName");
            editor.remove("gpxDistance");
            editor.remove("gpxElevationGain");
            editor.remove("gpxAvgSlope");
        }
    }

    public static void syncShoesWithFirebase(Context context, String myId) {
        if (myId == null || myId.isEmpty()) return;

        DatabaseReference shoesRef = FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("shoes")
                .child(myId);

        WorkoutDatabase db = WorkoutDatabase.getDatabase(context);
        ShoeDao shoeDao = db.shoeDao();

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                com.google.android.gms.tasks.Task<com.google.firebase.database.DataSnapshot> task = shoesRef.get();
                com.google.firebase.database.DataSnapshot snapshot = com.google.android.gms.tasks.Tasks.await(task);

                List<ShoeEntity> localShoes = shoeDao.getAllShoesSync();
                Map<String, ShoeEntity> localShoesMap = new HashMap<>();
                for (ShoeEntity s : localShoes) {
                    localShoesMap.put(s.getId(), s);
                }

                boolean changedLocal = false;
                if (snapshot.exists()) {
                    for (com.google.firebase.database.DataSnapshot child : snapshot.getChildren()) {
                        String shoeId = child.getKey();
                        if (shoeId == null) continue;

                        String name = child.child("name").getValue(String.class);
                        Double initialMil = child.child("initialMileage").getValue(Double.class);
                        Double limit = child.child("mileageLimit").getValue(Double.class);
                        Boolean retired = child.child("isRetired").getValue(Boolean.class);

                        ShoeEntity localShoe = localShoesMap.get(shoeId);
                        if (localShoe == null) {
                            ShoeEntity newShoe = new ShoeEntity();
                            newShoe.setId(shoeId);
                            newShoe.setName(name != null ? name : "Unknown");
                            newShoe.setInitialMileage(initialMil != null ? initialMil : 0.0);
                            newShoe.setMileageLimit(limit != null ? limit : 800.0);
                            newShoe.setRetired(retired != null ? retired : false);
                            shoeDao.insert(newShoe);
                            changedLocal = true;
                        } else {
                            double cInitialMil = initialMil != null ? initialMil : 0.0;
                            double cLimit = limit != null ? limit : 800.0;
                            boolean cRetired = retired != null ? retired : false;

                            if (!localShoe.getName().equals(name) ||
                                    localShoe.getInitialMileage() != cInitialMil ||
                                    localShoe.getMileageLimit() != cLimit ||
                                    localShoe.isRetired() != cRetired) {

                                localShoe.setName(name != null ? name : "Unknown");
                                localShoe.setInitialMileage(cInitialMil);
                                localShoe.setMileageLimit(cLimit);
                                localShoe.setRetired(cRetired);
                                shoeDao.update(localShoe);
                                changedLocal = true;
                            }
                        }
                    }
                }

                Map<String, Object> cloudShoesMap = new HashMap<>();
                List<ShoeEntity> updatedLocalShoes = shoeDao.getAllShoesSync();
                for (ShoeEntity s : updatedLocalShoes) {
                    Map<String, Object> sMap = new HashMap<>();
                    sMap.put("name", s.getName());
                    sMap.put("initialMileage", s.getInitialMileage());
                    sMap.put("mileageLimit", s.getMileageLimit());
                    sMap.put("isRetired", s.isRetired());
                    cloudShoesMap.put(s.getId(), sMap);
                }
                shoesRef.setValue(cloudShoesMap);
            } catch (Exception e) {
                android.util.Log.e("ShoeSync", "Error syncing shoes: " + e.getMessage());
            }
        });
    }
}
