package com.jostein.maratontrener;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;
import com.jostein.maratontrener.database.ShoeDao;
import com.jostein.maratontrener.database.ShoeEntity;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.RelativeLayout;
import android.widget.LinearLayout;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.content.res.ColorStateList;
import java.util.HashMap;
import java.util.Map;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

public class ProfileFragment extends Fragment {

    private EditText profileName, profileNickname, profileEmail, profileAge, profileWeight, profileMaxHR;
    private EditText profilePB10k, profilePBHalf, profilePBFull;
    private TextView textProfileNickname, textProfileEmail;
    private TextView textZone1, textZone2, textZone3, textZone4, textZone5;
    private TextView labelHRZones;
    private View badgeConsistencyFrame, badgeCenturyFrame;
    private ImageView imageBadgeConsistency, imageBadgeCentury;
    private TextView textBadgeConsistencyTitle, textBadgeConsistencyDesc, textBadgeCenturyDesc;
    private ShapeableImageView imageProfile;
    private RecyclerView recyclerShoes;
    private List<ShoeEntity> shoesList = new ArrayList<>();
    private ShoeAdapter shoeAdapter;
    private ShoeDao shoeDao;

    private String currentImagePath = null;
    private boolean hasUnsavedChanges = false;
    private boolean isInternalUpdate = false;
    private long fragmentStartTime = 0;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> { if (uri != null) saveImageToInternalStorage(uri); }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        fragmentStartTime = System.currentTimeMillis();

        textProfileNickname = view.findViewById(R.id.textProfileNickname);
        textProfileEmail = view.findViewById(R.id.textProfileEmail);
        imageProfile = view.findViewById(R.id.imageProfile);
        profileName = view.findViewById(R.id.profileName);
        profileNickname = view.findViewById(R.id.profileNickname);
        profileEmail = view.findViewById(R.id.profileEmail);
        profileAge = view.findViewById(R.id.profileAge);
        profileWeight = view.findViewById(R.id.profileWeight);
        profileMaxHR = view.findViewById(R.id.profileMaxHR);
        profilePB10k = view.findViewById(R.id.profilePB10k);
        profilePBHalf = view.findViewById(R.id.profilePBHalf);
        profilePBFull = view.findViewById(R.id.profilePBFull);
        textZone1 = view.findViewById(R.id.textZone1);
        textZone2 = view.findViewById(R.id.textZone2);
        textZone3 = view.findViewById(R.id.textZone3);
        textZone4 = view.findViewById(R.id.textZone4);
        textZone5 = view.findViewById(R.id.textZone5);
        labelHRZones = view.findViewById(R.id.labelHRZones);
        badgeConsistencyFrame = view.findViewById(R.id.badgeConsistencyFrame);
        badgeCenturyFrame = view.findViewById(R.id.badgeCenturyFrame);
        imageBadgeConsistency = view.findViewById(R.id.imageBadgeConsistency);
        imageBadgeCentury = view.findViewById(R.id.imageBadgeCentury);
        textBadgeConsistencyTitle = view.findViewById(R.id.textBadgeConsistencyTitle);
        textBadgeConsistencyDesc = view.findViewById(R.id.textBadgeConsistencyDesc);
        textBadgeCenturyDesc = view.findViewById(R.id.textBadgeCenturyDesc);

        setupTimeAutoFormat(profilePB10k); setupTimeAutoFormat(profilePBHalf); setupTimeAutoFormat(profilePBFull);
        view.findViewById(R.id.btnSaveProfile).setOnClickListener(v -> saveProfile());
        view.findViewById(R.id.btnDeleteData).setOnClickListener(v -> confirmDeleteData());
        view.findViewById(R.id.btnRaceHistory).setOnClickListener(v -> startActivity(new Intent(getActivity(), ArchiveActivity.class)));
        view.findViewById(R.id.btnRestoreFromCloud).setOnClickListener(v -> restoreFromCloud());
        imageProfile.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        view.findViewById(R.id.fabEditProfileImage).setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // Setup Dynamic Version Display
        TextView textProfileVersionDisplay = view.findViewById(R.id.textProfileVersionDisplay);
        try {
            String versionName = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
            int versionCode = (int) androidx.core.content.pm.PackageInfoCompat.getLongVersionCode(requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0));
            textProfileVersionDisplay.setText("Version " + versionName + " (" + versionCode + ")");
        } catch (Exception e) {
            textProfileVersionDisplay.setText("Version 3.8.1 (381)");
        }

        recyclerShoes = view.findViewById(R.id.recyclerShoes);
        recyclerShoes.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));
        shoeDao = WorkoutDatabase.getDatabase(requireContext()).shoeDao();
        view.findViewById(R.id.btnAddShoe).setOnClickListener(v -> showShoeDialog(null));

        setupChangeTracking(); loadProfile();
        return view;
    }

    private void setupChangeTracking() {
        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isInternalUpdate || (System.currentTimeMillis() - fragmentStartTime < 1000)) return;
                hasUnsavedChanges = true;
            }
        };
        profileName.addTextChangedListener(watcher); profileNickname.addTextChangedListener(watcher); profileEmail.addTextChangedListener(watcher);
        profileAge.addTextChangedListener(watcher); profileWeight.addTextChangedListener(watcher); profileMaxHR.addTextChangedListener(watcher);
        profilePB10k.addTextChangedListener(watcher); profilePBHalf.addTextChangedListener(watcher); profilePBFull.addTextChangedListener(watcher);
    }

    private void setupTimeAutoFormat(EditText editText) {
        editText.addTextChangedListener(new TextWatcher() {
            private boolean isUpdating = false;
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (isUpdating) return;
                String input = s.toString().replaceAll("[^\\d]", ""); if (input.length() > 6) input = input.substring(0, 6);
                StringBuilder f = new StringBuilder();
                if (!input.isEmpty()) {
                    if (input.length() <= 2) f.append(input);
                    else if (input.length() <= 4) f.append(input.substring(0, input.length() - 2)).append(":").append(input.substring(input.length() - 2));
                    else f.append(input.substring(0, input.length() - 4)).append(":").append(input.substring(input.length() - 4, input.length() - 2)).append(":").append(input.substring(input.length() - 2));
                }
                isUpdating = true; editText.setText(f.toString()); editText.setSelection(f.length()); isUpdating = false;
            }
        });
    }

    private void saveImageToInternalStorage(Uri uri) {
        try {
            InputStream is = requireContext().getContentResolver().openInputStream(uri);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);
            is.close();

            int srcWidth = options.outWidth;
            int srcHeight = options.outHeight;
            int maxDimension = 500;
            int inSampleSize = 1;
            if (srcWidth > maxDimension || srcHeight > maxDimension) {
                int halfWidth = srcWidth / 2;
                int halfHeight = srcHeight / 2;
                while ((halfWidth / inSampleSize) >= maxDimension && (halfHeight / inSampleSize) >= maxDimension) {
                    inSampleSize *= 2;
                }
            }

            options.inJustDecodeBounds = false;
            options.inSampleSize = inSampleSize;
            is = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
            is.close();

            if (bitmap != null) {
                if (bitmap.getWidth() > maxDimension || bitmap.getHeight() > maxDimension) {
                    float ratio = Math.min((float) maxDimension / bitmap.getWidth(), (float) maxDimension / bitmap.getHeight());
                    int targetWidth = Math.round(ratio * bitmap.getWidth());
                    int targetHeight = Math.round(ratio * bitmap.getHeight());
                    Bitmap scaled = Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
                    if (scaled != bitmap) {
                        bitmap.recycle();
                        bitmap = scaled;
                    }
                }

                File dir = new File(requireContext().getFilesDir(), "profile_images");
                if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, "profile_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream os = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, os);
                os.close();
                bitmap.recycle();

                currentImagePath = file.getAbsolutePath();
                hasUnsavedChanges = true;
                displayProfileImage(currentImagePath);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayProfileImage(String path) {
        if (path != null && new File(path).exists()) {
            BitmapFactory.Options o = new BitmapFactory.Options(); o.inSampleSize = 2;
            Bitmap b = BitmapFactory.decodeFile(path, o);
            if (b != null) { imageProfile.setImageBitmap(b); imageProfile.setPadding(0, 0, 0, 0); imageProfile.setImageTintList(null); imageProfile.setColorFilter(null); }
        } else {
            imageProfile.setImageResource(R.drawable.ic_person);
            imageProfile.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
            imageProfile.setPadding(spToPx(16), spToPx(16), spToPx(16), spToPx(16));
        }
    }

    private void loadProfile() {
        isInternalUpdate = true;
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext());
        profileName.setText(prefs.getString("userName", ""));
        profileNickname.setText(prefs.getString("userNickname", "Athlete"));
        profileEmail.setText(prefs.getString("userEmail", "athlete@example.com"));
        profileAge.setText(prefs.getString("userAge", ""));
        profileWeight.setText(prefs.getString("userWeight", ""));
        profileMaxHR.setText(prefs.getString("userMaxHR", ""));
        profilePB10k.setText(prefs.getString("pb10k", ""));
        profilePBHalf.setText(prefs.getString("pbHalf", ""));
        profilePBFull.setText(prefs.getString("pbFull", ""));
        textProfileNickname.setText(profileNickname.getText());
        textProfileEmail.setText(profileEmail.getText());
        if (!hasUnsavedChanges) currentImagePath = prefs.getString("profileImagePath", null);
        displayProfileImage(currentImagePath); updateHRZonesDisplay();
        loadShoes();
        loadAchievements();
        profileName.postDelayed(() -> isInternalUpdate = false, 500);
    }

    private void updateHRZonesDisplay() {
        int maxHR = 0; 
        String maxStr = profileMaxHR.getText().toString();
        String ageStr = profileAge.getText().toString();
        boolean isEstimated = false;

        if (!maxStr.isEmpty()) {
            try { maxHR = Integer.parseInt(maxStr); } catch (Exception ignored) {}
        } else if (!ageStr.isEmpty()) {
            try { 
                int age = Integer.parseInt(ageStr);
                maxHR = (int) Math.round(208 - (0.7 * age)); 
                isEstimated = true;
            } catch (Exception ignored) {} 
        }

        if (labelHRZones != null) {
            if (isEstimated) {
                labelHRZones.setText("HEART RATE ZONES (AGE ESTIMATED)");
            } else {
                labelHRZones.setText("HEART RATE ZONES");
            }
        }

        if (maxHR > 0) {
            textZone1.setText(String.format(Locale.getDefault(), "Zone 1: Recovery (%d - %d)", (int)(maxHR * 0.5), (int)(maxHR * 0.6)));
            textZone2.setText(String.format(Locale.getDefault(), "Zone 2: Aerobic (%d - %d)", (int)(maxHR * 0.6), (int)(maxHR * 0.7)));
            textZone3.setText(String.format(Locale.getDefault(), "Zone 3: Tempo (%d - %d)", (int)(maxHR * 0.7), (int)(maxHR * 0.8)));
            textZone4.setText(String.format(Locale.getDefault(), "Zone 4: Threshold (%d - %d)", (int)(maxHR * 0.8), (int)(maxHR * 0.9)));
            textZone5.setText(String.format(Locale.getDefault(), "Zone 5: Anaerobic (%d - %d)", (int)(maxHR * 0.9), maxHR));
        }
    }

    private void loadAchievements() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                WorkoutDatabase db = WorkoutDatabase.getDatabase(requireContext());
                SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext());
                long planStartDate = prefs.getLong("planStartDate", 0);

                List<WorkoutEntity> allWorkouts = db.workoutDao().getAllWorkoutsSync();
                List<WorkoutEntity> filteredWorkouts = new java.util.ArrayList<>();
                for (WorkoutEntity w : allWorkouts) {
                    if (planStartDate == 0 || w.getScheduledDate() >= planStartDate) {
                        filteredWorkouts.add(w);
                    }
                }
                if (filteredWorkouts.isEmpty()) {
                    filteredWorkouts = allWorkouts;
                }

                int totalWorkouts = filteredWorkouts.size();
                int completedWorkouts = 0;
                double totalDistance = 0.0;
                for (WorkoutEntity w : filteredWorkouts) {
                    if (w.isCompleted()) {
                        completedWorkouts++;
                    }
                }
                for (WorkoutEntity w : allWorkouts) {
                    if (w.isCompleted()) {
                        totalDistance += w.getDistance();
                    }
                }

                if (getActivity() == null) return;
                
                final int finalTotal = totalWorkouts;
                final int finalCompleted = completedWorkouts;
                final double finalDistance = totalDistance;

                getActivity().runOnUiThread(() -> {
                    // Update Consistency Badge
                    int consistency = 0;
                    if (finalTotal > 0) {
                        consistency = (int) Math.round((double) finalCompleted / finalTotal * 100);
                    }
                    
                    String badgeTitle;
                    int badgeColor;
                    if (consistency < 50) {
                        badgeTitle = "Slob!";
                        badgeColor = 0xFFFF3B30; // Red
                    } else if (consistency < 75) {
                        badgeTitle = "That all?";
                        badgeColor = 0xFFFF9500; // Orange
                    } else if (consistency < 90) {
                        badgeTitle = "Keep going!";
                        badgeColor = 0xFFFFD60A; // Yellow
                    } else {
                        badgeTitle = "Consistency King";
                        badgeColor = 0xFFCCFF00; // Neon Lime
                    }
                    
                    textBadgeConsistencyTitle.setText(badgeTitle);
                    textBadgeConsistencyDesc.setText(consistency + "% consistency");
                    
                    badgeConsistencyFrame.setBackgroundTintList(ColorStateList.valueOf(badgeColor));
                    imageBadgeConsistency.setImageTintList(ColorStateList.valueOf(badgeColor));
                    
                    // Update Century Club Badge
                    textBadgeCenturyDesc.setText(String.format(Locale.getDefault(), "%.1f / 100 km", finalDistance));
                    if (finalDistance >= 100.0) {
                        int activeColor = 0xFFCCFF00; // Neon Lime
                        badgeCenturyFrame.setBackgroundTintList(ColorStateList.valueOf(activeColor));
                        imageBadgeCentury.setImageTintList(ColorStateList.valueOf(activeColor));
                    } else {
                        int lockedColor = 0x88FFFFFF; // Semi-transparent white
                        badgeCenturyFrame.setBackgroundTintList(ColorStateList.valueOf(lockedColor));
                        imageBadgeCentury.setImageTintList(ColorStateList.valueOf(lockedColor));
                    }
                });
            } catch (Exception ignored) {}
        });
    }

    private void saveProfile() {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext());
        String nick = profileNickname.getText().toString();
        prefs.edit().putString("userName", profileName.getText().toString()).putString("userNickname", nick).putString("userEmail", profileEmail.getText().toString())
            .putString("userAge", profileAge.getText().toString()).putString("userWeight", profileWeight.getText().toString()).putString("userMaxHR", profileMaxHR.getText().toString())
            .putString("pb10k", profilePB10k.getText().toString()).putString("pbHalf", profilePBHalf.getText().toString()).putString("pbFull", profilePBFull.getText().toString())
            .putString("profileImagePath", currentImagePath)
            .putLong("profileLastUpdate", System.currentTimeMillis()).apply();
        SecurityUtils.getEncryptedPrefs(requireContext(), "BuddyPrefs").edit().putString("my_name", nick).apply();
        textProfileNickname.setText(nick); textProfileEmail.setText(profileEmail.getText().toString());
        updateHRZonesDisplay(); hasUnsavedChanges = false; Toast.makeText(requireContext(), "Profile Saved", Toast.LENGTH_SHORT).show();

        // Sync profile details (including weight/age) with Firebase immediately
        WorkoutUtils.uploadWorkoutsToFirebase(requireContext());
        
        // Return to Dashboard after saving
        if (getActivity() instanceof MainContainerActivity) {
            ((MainContainerActivity) getActivity()).switchToTab(R.id.nav_home);
        }
    }

    private void confirmDeleteData() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext()).setTitle("PERMANENT DELETE").setMessage("This will wipe all your training logs and profile info from this phone AND the cloud. This cannot be undone. Are you sure?")
                .setPositiveButton("DELETE EVERYTHING", (dialog, which) -> performNuclearDelete()).setNegativeButton("Cancel", null).show();
    }

    private void performNuclearDelete() {
        SharedPreferences buddyPrefs = requireContext().getSharedPreferences("BuddyPrefs", Context.MODE_PRIVATE);
        String myBuddyId = buddyPrefs.getString("my_id", null);
        if (myBuddyId != null) try { FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/").getReference("profiles").child(myBuddyId).removeValue(); } catch (Exception ignored) {}
        requireContext().getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).edit().clear().apply();
        SecurityUtils.getEncryptedPrefs(requireContext()).edit().clear().apply();
        requireContext().getSharedPreferences("BuddyPrefs", Context.MODE_PRIVATE).edit().clear().apply();
        requireContext().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit().clear().apply();
        com.jostein.maratontrener.database.WorkoutDatabase.getDatabase(requireContext()).clearAllTables();
        Toast.makeText(requireContext(), "All data deleted. Restarting...", Toast.LENGTH_LONG).show();
        requireActivity().finishAffinity(); System.exit(0);
    }

    private void restoreFromCloud() {
        SharedPreferences buddyPrefs = SecurityUtils.getEncryptedPrefs(requireContext(), "BuddyPrefs");
        String myId = buddyPrefs.getString("my_id", "CH020721");
        if (!buddyPrefs.contains("my_id")) {
            buddyPrefs.edit().putString("my_id", myId).apply();
        }

        Toast.makeText(requireContext(), "Restoring data from cloud...", Toast.LENGTH_SHORT).show();

        // 1. Fetch Profile info to restore event metadata (Name, Distance, Date)
        DatabaseReference profileRef = FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("profiles")
                .child(myId);

        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    try {
                        SharedPreferences userPrefs = SecurityUtils.getEncryptedPrefs(requireContext());
                        SharedPreferences.Editor editor = userPrefs.edit();

                        String name = snapshot.child("name").getValue(String.class);
                        if (name != null && !name.isEmpty()) {
                            editor.putString("userName", name);
                        }
                        String nickname = snapshot.child("nickname").getValue(String.class);
                        if (nickname == null || nickname.isEmpty()) nickname = name;
                        if (nickname != null && !nickname.isEmpty()) {
                            editor.putString("userNickname", nickname);
                            buddyPrefs.edit().putString("my_name", nickname).apply();
                        }
                        String email = snapshot.child("email").getValue(String.class);
                        if (email != null) {
                            editor.putString("userEmail", email);
                        }
                        String location = snapshot.child("eventLocation").getValue(String.class);
                        if (location != null) {
                            editor.putString("eventLocation", location);
                        }

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

                        // Decode and restore local profile image from Firebase Base64 avatar
                        String avatarBase64 = snapshot.child("avatar").getValue(String.class);
                        if (avatarBase64 != null && avatarBase64.startsWith("data:image")) {
                            try {
                                int commaIdx = avatarBase64.indexOf(",");
                                if (commaIdx != -1) {
                                    String cleanBase64 = avatarBase64.substring(commaIdx + 1);
                                    byte[] decodedBytes = android.util.Base64.decode(cleanBase64, android.util.Base64.DEFAULT);
                                    
                                    java.io.File dir = new java.io.File(requireContext().getFilesDir(), "profile_images");
                                    if (!dir.exists()) dir.mkdirs();
                                    java.io.File file = new java.io.File(dir, "profile.jpg");
                                    java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
                                    fos.write(decodedBytes);
                                    fos.close();
                                    
                                    editor.putString("profileImagePath", file.getAbsolutePath());
                                }
                            } catch (Exception ignored) {}
                        }

                        String currentRace = snapshot.child("currentRace").getValue(String.class);
                        if (currentRace != null && !currentRace.isEmpty()) {
                            String raceInfo = currentRace;
                            String dateStr = null;
                            if (currentRace.contains(": ")) {
                                int colonIdx = currentRace.indexOf(": ");
                                raceInfo = currentRace.substring(0, colonIdx);
                                dateStr = currentRace.substring(colonIdx + 2);
                            }

                            if (dateStr != null) {
                                try {
                                    SimpleDateFormat raceSdf = new SimpleDateFormat("d MMM yyyy", Locale.US);
                                    Date parsedEventDate = raceSdf.parse(dateStr);
                                    if (parsedEventDate != null) {
                                        editor.putLong("eventDate", parsedEventDate.getTime());
                                    }
                                } catch (Exception e) {
                                    android.util.Log.e("RestoreFromCloud", "Failed to parse event date: " + dateStr, e);
                                }
                            }

                            if (raceInfo.contains(" - ")) {
                                String[] parts = raceInfo.split(" - ");
                                editor.putString("eventName", parts[0].trim());
                                editor.putString("eventType", parts[1].trim());
                            } else {
                                editor.putString("eventName", raceInfo.trim());
                            }
                        }
                        WorkoutUtils.restoreGpxFromSnapshot(editor, snapshot);
                        editor.apply();
                    } catch (Exception e) {
                        android.util.Log.e("RestoreFromCloud", "Error parsing currentRace", e);
                    }
                }
                
                // Now fetch workouts
                restoreWorkoutsFromCloud(myId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getActivity() != null) {
                    Toast.makeText(requireContext(), "Failed to restore profile: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void restoreWorkoutsFromCloud(String myId) {
        DatabaseReference workoutsRef = FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/")
                .getReference("workouts")
                .child(myId);

        workoutsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    if (getActivity() != null) {
                        Toast.makeText(requireContext(), "No training workouts found in the cloud.", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                List<WorkoutEntity> workoutsToInsert = new ArrayList<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

                for (DataSnapshot child : snapshot.getChildren()) {
                    try {
                        String dateStr = child.child("scheduledDate").getValue(String.class);
                        long dateMs = System.currentTimeMillis();
                        if (dateStr != null) {
                            Date parsedDate = sdf.parse(dateStr);
                            if (parsedDate != null) {
                                dateMs = parsedDate.getTime();
                            }
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
                    } catch (Exception e) {
                        android.util.Log.e("RestoreFromCloud", "Error parsing workout: " + e.getMessage());
                    }
                }

                if (workoutsToInsert.isEmpty()) {
                    if (getActivity() != null) {
                        Toast.makeText(requireContext(), "No valid workouts could be restored.", Toast.LENGTH_LONG).show();
                    }
                    return;
                }

                // Save to local database
                Executors.newSingleThreadExecutor().execute(() -> {
                    try {
                        WorkoutDao dao = WorkoutDatabase.getDatabase(requireContext()).workoutDao();
                        dao.deleteAll();
                        dao.insertAll(workoutsToInsert);

                        WorkoutUtils.syncShoesWithFirebase(requireContext(), myId);

                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                Toast.makeText(requireContext(), "Restored " + workoutsToInsert.size() + " workouts successfully!", Toast.LENGTH_LONG).show();
                                // Reload local profile fields on current fragment tab
                                loadProfile();
                                // Go back to Home tab to show restored plan
                                if (getActivity() instanceof MainContainerActivity) {
                                    ((MainContainerActivity) getActivity()).switchToTab(R.id.nav_home);
                                }
                            });
                        }
                    } catch (Exception e) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> 
                                Toast.makeText(requireContext(), "Error saving to database: " + e.getMessage(), Toast.LENGTH_LONG).show()
                            );
                        }
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getActivity() != null) {
                    Toast.makeText(requireContext(), "Cancelled: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private int spToPx(int sp) { return (int) (sp * getResources().getDisplayMetrics().scaledDensity); }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private void loadShoes() {
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                List<ShoeEntity> list = shoeDao.getAllShoesSync();
                WorkoutDao workoutDao = WorkoutDatabase.getDatabase(requireContext()).workoutDao();
                Map<String, Double> mileageMap = new HashMap<>();
                for (ShoeEntity s : list) {
                    double mileage = workoutDao.getMileageForShoeSync(s.getId());
                    mileageMap.put(s.getId(), mileage);
                }

                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        shoesList.clear();
                        shoesList.addAll(list);
                        if (shoeAdapter == null) {
                            shoeAdapter = new ShoeAdapter(shoesList, mileageMap);
                            recyclerShoes.setAdapter(shoeAdapter);
                        } else {
                            shoeAdapter.updateData(shoesList, mileageMap);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void showShoeDialog(final ShoeEntity existingShoe) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext(), R.style.CustomDatePickerDialogTheme);
        builder.setTitle(existingShoe == null ? "Add Running Shoe" : "Edit Running Shoe");

        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dpToPx(24), dpToPx(16), dpToPx(24), dpToPx(16));

        EditText editName = new EditText(requireContext());
        editName.setHint("Shoe Model Name");
        editName.setTextColor(0xFFFFFFFF);
        editName.setHintTextColor(0x80FFFFFF);
        if (existingShoe != null) editName.setText(existingShoe.getName());
        layout.addView(editName);

        EditText editInitial = new EditText(requireContext());
        editInitial.setHint("Initial Mileage (km) - e.g. 150");
        editInitial.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL);
        editInitial.setTextColor(0xFFFFFFFF);
        editInitial.setHintTextColor(0x80FFFFFF);
        LinearLayout.LayoutParams lpInit = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lpInit.setMargins(0, dpToPx(12), 0, 0);
        editInitial.setLayoutParams(lpInit);
        if (existingShoe != null) editInitial.setText(String.valueOf(existingShoe.getInitialMileage()));
        else editInitial.setText("0.0");
        layout.addView(editInitial);

        EditText editLimit = new EditText(requireContext());
        editLimit.setHint("Mileage Limit (km) - default 800");
        editLimit.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        editLimit.setTextColor(0xFFFFFFFF);
        editLimit.setHintTextColor(0x80FFFFFF);
        editLimit.setLayoutParams(lpInit);
        if (existingShoe != null) editLimit.setText(String.valueOf((int)existingShoe.getMileageLimit()));
        else editLimit.setText("800");
        layout.addView(editLimit);

        CheckBox checkRetired = new CheckBox(requireContext());
        checkRetired.setText("Retire this shoe");
        checkRetired.setTextColor(0xFFFFFFFF);
        checkRetired.setButtonTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
        checkRetired.setLayoutParams(lpInit);
        if (existingShoe != null) checkRetired.setChecked(existingShoe.isRetired());
        layout.addView(checkRetired);

        builder.setView(layout);

        builder.setPositiveButton("Save", (dialog, which) -> {
            String name = editName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(requireContext(), "Name cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }

            double initial = 0.0;
            try {
                initial = Double.parseDouble(editInitial.getText().toString());
            } catch (Exception ignored) {}

            double limit = 800.0;
            try {
                limit = Double.parseDouble(editLimit.getText().toString());
            } catch (Exception ignored) {}

            boolean retired = checkRetired.isChecked();

            ShoeEntity shoe = existingShoe;
            boolean isNew = (shoe == null);
            if (isNew) {
                shoe = new ShoeEntity();
                shoe.setId("SHOE_" + System.currentTimeMillis());
            }
            shoe.setName(name);
            shoe.setInitialMileage(initial);
            shoe.setMileageLimit(limit);
            shoe.setRetired(retired);

            final ShoeEntity finalShoe = shoe;
            Executors.newSingleThreadExecutor().execute(() -> {
                if (isNew) shoeDao.insert(finalShoe);
                else shoeDao.update(finalShoe);

                SharedPreferences buddyPrefs = SecurityUtils.getEncryptedPrefs(requireContext(), "BuddyPrefs");
                String myId = buddyPrefs.getString("my_id", null);
                if (myId != null) {
                    WorkoutUtils.syncShoesWithFirebase(requireContext(), myId);
                }

                loadShoes();
            });
        });

        builder.setNegativeButton("Cancel", null);

        if (existingShoe != null) {
            builder.setNeutralButton("Delete", (dialog, which) -> {
                Executors.newSingleThreadExecutor().execute(() -> {
                    shoeDao.delete(existingShoe);

                    SharedPreferences buddyPrefs = SecurityUtils.getEncryptedPrefs(requireContext(), "BuddyPrefs");
                    String myId = buddyPrefs.getString("my_id", null);
                    if (myId != null) {
                        WorkoutUtils.syncShoesWithFirebase(requireContext(), myId);
                    }

                    loadShoes();
                });
            });
        }

        builder.show();
    }

    private class ShoeAdapter extends RecyclerView.Adapter<ShoeAdapter.ShoeViewHolder> {
        private final List<ShoeEntity> list;
        private final Map<String, Double> mileageMap;

        public ShoeAdapter(List<ShoeEntity> list, Map<String, Double> mileageMap) {
            this.list = list;
            this.mileageMap = mileageMap;
        }

        public void updateData(List<ShoeEntity> newList, Map<String, Double> newMileageMap) {
            this.list.clear();
            this.list.addAll(newList);
            this.mileageMap.clear();
            this.mileageMap.putAll(newMileageMap);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ShoeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout container = new LinearLayout(parent.getContext());
            container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            container.setOrientation(LinearLayout.VERTICAL);
            container.setPadding(dpToPx(16), dpToPx(12), dpToPx(16), dpToPx(12));

            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setColor(0x1AFFFFFF);
            gd.setCornerRadius(dpToPx(12));
            gd.setStroke(dpToPx(1), 0x1AFFFFFF);
            container.setBackground(gd);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dpToPx(12));
            container.setLayoutParams(lp);

            return new ShoeViewHolder(container);
        }

        @Override
        public void onBindViewHolder(@NonNull ShoeViewHolder holder, int position) {
            ShoeEntity shoe = list.get(position);
            Double runMileage = mileageMap.get(shoe.getId());
            double totalMileage = shoe.getInitialMileage() + (runMileage != null ? runMileage : 0.0);

            holder.bind(shoe, totalMileage);
        }

        @Override
        public int getItemCount() {
            return list.size();
        }

        class ShoeViewHolder extends RecyclerView.ViewHolder {
            private final TextView textName;
            private final TextView textMileage;
            private final TextView textStatus;
            private final View progressBar;
            private final View progressBg;
            private final Button btnEdit;

            public ShoeViewHolder(@NonNull View itemView) {
                super(itemView);

                LinearLayout layout = (LinearLayout) itemView;

                RelativeLayout topRow = new RelativeLayout(layout.getContext());
                RelativeLayout.LayoutParams lpTop = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                topRow.setLayoutParams(lpTop);

                textName = new TextView(layout.getContext());
                textName.setTextColor(0xFFFFFFFF);
                textName.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 15);
                textName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                RelativeLayout.LayoutParams lpName = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                lpName.addRule(RelativeLayout.ALIGN_PARENT_START);
                topRow.addView(textName, lpName);

                textStatus = new TextView(layout.getContext());
                textStatus.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 10);
                textStatus.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                textStatus.setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2));
                RelativeLayout.LayoutParams lpStatus = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                lpStatus.addRule(RelativeLayout.ALIGN_PARENT_END);
                topRow.addView(textStatus, lpStatus);

                layout.addView(topRow);

                LinearLayout middleRow = new LinearLayout(layout.getContext());
                middleRow.setOrientation(LinearLayout.HORIZONTAL);
                LinearLayout.LayoutParams lpMiddle = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                lpMiddle.setMargins(0, dpToPx(8), 0, dpToPx(4));
                middleRow.setLayoutParams(lpMiddle);

                textMileage = new TextView(layout.getContext());
                textMileage.setTextColor(0xFF8E8E93);
                textMileage.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 12);
                middleRow.addView(textMileage);

                layout.addView(middleRow);

                progressBg = new View(layout.getContext());
                LinearLayout.LayoutParams lpBg = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(6));
                lpBg.setMargins(0, dpToPx(4), 0, dpToPx(8));
                progressBg.setLayoutParams(lpBg);

                android.graphics.drawable.GradientDrawable bgDrawable = new android.graphics.drawable.GradientDrawable();
                bgDrawable.setColor(0x1AFFFFFF);
                bgDrawable.setCornerRadius(dpToPx(3));
                progressBg.setBackground(bgDrawable);

                RelativeLayout progressWrapper = new RelativeLayout(layout.getContext());
                progressWrapper.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(6)));

                progressBar = new View(layout.getContext());
                progressWrapper.addView(progressBar);
                progressWrapper.setBackground(bgDrawable);

                layout.addView(progressWrapper);

                RelativeLayout bottomRow = new RelativeLayout(layout.getContext());
                RelativeLayout.LayoutParams lpBottom = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                bottomRow.setLayoutParams(lpBottom);

                btnEdit = new Button(layout.getContext(), null, android.R.attr.borderlessButtonStyle);
                btnEdit.setText("EDIT");
                btnEdit.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 11);
                btnEdit.setTextColor(0xFFCCFF00);
                btnEdit.setPadding(0, 0, 0, 0);
                btnEdit.setMinWidth(0);
                btnEdit.setMinHeight(0);
                RelativeLayout.LayoutParams lpEdit = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                lpEdit.addRule(RelativeLayout.ALIGN_PARENT_END);
                bottomRow.addView(btnEdit, lpEdit);

                layout.addView(bottomRow);
            }

            public void bind(ShoeEntity shoe, double totalMileage) {
                textName.setText(shoe.getName());

                if (shoe.isRetired()) {
                    textStatus.setText("RETIRED");
                    textStatus.setTextColor(0xFF8E8E93);
                    android.graphics.drawable.GradientDrawable retiredGd = new android.graphics.drawable.GradientDrawable();
                    retiredGd.setColor(0x1AFFFFFF);
                    retiredGd.setCornerRadius(dpToPx(10));
                    textStatus.setBackground(retiredGd);
                } else {
                    textStatus.setText("ACTIVE");
                    textStatus.setTextColor(0xFFCCFF00);
                    android.graphics.drawable.GradientDrawable activeGd = new android.graphics.drawable.GradientDrawable();
                    activeGd.setColor(0x26CCFF00);
                    activeGd.setCornerRadius(dpToPx(10));
                    textStatus.setBackground(activeGd);
                }

                textMileage.setText(String.format(Locale.getDefault(), "%.1f km / %.0f km limit", totalMileage, shoe.getMileageLimit()));

                double pct = shoe.getMileageLimit() > 0 ? (totalMileage / shoe.getMileageLimit()) : 0.0;
                if (pct > 1.0) pct = 1.0;

                final double finalPct = pct;
                progressBar.post(() -> {
                    int wrapperWidth = ((View) progressBar.getParent()).getWidth();
                    ViewGroup.LayoutParams lp = progressBar.getLayoutParams();
                    lp.width = (int) (wrapperWidth * finalPct);
                    progressBar.setLayoutParams(lp);
                });

                android.graphics.drawable.GradientDrawable progressGd = new android.graphics.drawable.GradientDrawable();
                progressGd.setCornerRadius(dpToPx(3));
                if (pct < 0.70) {
                    progressGd.setColor(0xFF34C759);
                } else if (pct < 1.0) {
                    progressGd.setColor(0xFFFF9500);
                } else {
                    progressGd.setColor(0xFFFF3B30);
                }
                progressBar.setBackground(progressGd);

                btnEdit.setOnClickListener(v -> showShoeDialog(shoe));
            }
        }
    }
}