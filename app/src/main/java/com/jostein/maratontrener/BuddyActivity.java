package com.jostein.maratontrener;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.jostein.maratontrener.adapters.FriendAdapter;
import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;
import com.jostein.maratontrener.models.FriendProfile;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;

public class BuddyActivity extends AppCompatActivity {

    private EditText editBuddyId;
    private TextView textYourId;
    private Button btnAddBuddy, btnShareId;
    private RecyclerView recyclerBuddies;
    private FriendAdapter friendAdapter;
    private List<FriendProfile> friendProfiles = new ArrayList<>();

    private String myId;
    private WorkoutDao workoutDao;
    private DatabaseReference dbRef;
    private Set<String> followedIds = new HashSet<>();
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_buddy);

        try {
            dbRef = FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/").getReference("profiles");
        } catch (Exception e) {
            // Firebase not initialized yet
        }

        // Setup Toolbar
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarBuddy);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        findViewById(R.id.btnBackBuddy).setOnClickListener(v -> finish());

        workoutDao = WorkoutDatabase.getDatabase(this).workoutDao();

        editBuddyId = findViewById(R.id.editBuddyId);
        textYourId = findViewById(R.id.textYourId);
        btnAddBuddy = findViewById(R.id.btnAddBuddy);
        btnShareId = findViewById(R.id.btnShareId);
        
        recyclerBuddies = findViewById(R.id.recyclerBuddies);
        recyclerBuddies.setLayoutManager(new LinearLayoutManager(this));
        friendAdapter = new FriendAdapter();
        recyclerBuddies.setAdapter(friendAdapter);

        bottomNavigationView = findViewById(R.id.bottomNavigation);
        setupNavigation();

        loadMyId();
        loadFollowedBuddies();
        startListeningForFollowers();
        saveAndUploadProfile();

        // Make ID copyable
        textYourId.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Buddy ID", myId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, R.string.id_copied, Toast.LENGTH_SHORT).show();
        });

        btnAddBuddy.setOnClickListener(v -> trackBuddy());
        btnShareId.setOnClickListener(v -> shareMyId());

        findViewById(R.id.imageProfileBuddy).setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        loadProfileImage();
    }

    private void setupNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_buddies);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_buddies) return true;
            
            Intent intent = null;
            if (id == R.id.nav_home) intent = new Intent(this, HomeActivity.class);
            else if (id == R.id.nav_log) intent = new Intent(this, MainActivity.class);
            else if (id == R.id.nav_stats) intent = new Intent(this, ProgressActivity.class);
            else if (id == R.id.nav_profile) intent = new Intent(this, ProfileActivity.class);
            
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (bottomNavigationView != null) {
            bottomNavigationView.getMenu().findItem(R.id.nav_buddies).setChecked(true);
        }
        loadProfileImage();
    }

    private void loadProfileImage() {
        try {
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this);
            String path = prefs.getString("profileImagePath", null);
            com.google.android.material.imageview.ShapeableImageView imageProfileBuddy = findViewById(R.id.imageProfileBuddy);

            if (path != null && new File(path).exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                Bitmap b = BitmapFactory.decodeFile(path, options);
                
                if (b != null) {
                    imageProfileBuddy.setImageBitmap(b);
                    imageProfileBuddy.setPadding(0, 0, 0, 0);
                    imageProfileBuddy.setImageTintList(null);
                    imageProfileBuddy.setColorFilter(null);
                }
            } else {
                imageProfileBuddy.setImageResource(R.drawable.ic_person);
                imageProfileBuddy.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
                imageProfileBuddy.setPadding(spToPx(4), spToPx(4), spToPx(4), spToPx(4));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }

    private void shareMyId() {
        SharedPreferences userPrefs = SecurityUtils.getEncryptedPrefs(this);
        String nickname = userPrefs.getString("userNickname", "Runner");
        String shareBody = getString(R.string.share_body_template, myId, nickname);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject));
        intent.putExtra(Intent.EXTRA_TEXT, shareBody);

        try {
            int qrResId = getResources().getIdentifier("qr_code", "drawable", getPackageName());
            if (qrResId != 0) {
                File cachePath = new File(getCacheDir(), "images");
                cachePath.mkdirs();
                File file = new File(cachePath, "qr_invite.png");
                FileOutputStream stream = new FileOutputStream(file);
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), qrResId);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                stream.close();
                Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else {
                intent.setType("text/plain");
            }
        } catch (Exception e) {
            intent.setType("text/plain");
        }
        startActivity(Intent.createChooser(intent, getString(R.string.invite_via)));
    }

    private void loadMyId() {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this, "BuddyPrefs");
        myId = prefs.getString("my_id", "CH020721");
        textYourId.setText(myId);
    }

    private void saveAndUploadProfile() {
        WorkoutUtils.uploadWorkoutsToFirebase(this);
    }

    private void trackBuddy() {
        String buddyId = editBuddyId.getText().toString().trim().toUpperCase();
        if (buddyId.isEmpty() || followedIds.contains(buddyId)) return;
        
        // 1. Follow them locally
        followedIds.add(buddyId);
        saveFollowedIds();
        startListeningToBuddy(buddyId);
        
        // 2. Tell Firebase I am following them (so they can add me back)
        if (dbRef != null && myId != null) {
            dbRef.child(buddyId).child("followers").child(myId).setValue(true);
        }
        
        editBuddyId.setText("");
    }

    private void startListeningForFollowers() {
        if (dbRef == null || myId == null) return;
        
        dbRef.child(myId).child("followers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    boolean changed = false;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String followerId = child.getKey();
                        if (followerId != null && !followedIds.contains(followerId)) {
                            followedIds.add(followerId);
                            startListeningToBuddy(followerId);
                            changed = true;
                        }
                    }
                    if (changed) {
                        saveFollowedIds();
                        Toast.makeText(BuddyActivity.this, "New training buddy added!", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void startListeningToBuddy(String buddyId) {
        if (dbRef == null) return;
        dbRef.child(buddyId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    FriendProfile profile = snapshot.getValue(FriendProfile.class);
                    if (profile != null) {
                        profile.id = snapshot.getKey();
                        updateFriendInList(profile);
                    }
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void updateFriendInList(FriendProfile updatedProfile) {
        boolean found = false;
        for (int i = 0; i < friendProfiles.size(); i++) {
            if (friendProfiles.get(i).id != null && friendProfiles.get(i).id.equals(updatedProfile.id)) {
                friendProfiles.set(i, updatedProfile);
                found = true; break;
            }
        }
        if (!found) friendProfiles.add(updatedProfile);
        runOnUiThread(() -> friendAdapter.setFriends(new ArrayList<>(friendProfiles)));
    }

    private void loadFollowedBuddies() {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(this, "BuddyPrefs");
        Set<String> savedIds = prefs.getStringSet("followed_ids", new HashSet<>());
        followedIds = new HashSet<>(savedIds);
        for (String id : followedIds) startListeningToBuddy(id);
    }

    private void saveFollowedIds() {
        SecurityUtils.getEncryptedPrefs(this, "BuddyPrefs").edit().putStringSet("followed_ids", followedIds).apply();
    }
}
