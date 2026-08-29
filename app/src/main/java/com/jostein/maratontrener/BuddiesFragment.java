package com.jostein.maratontrener;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.jostein.maratontrener.adapters.FriendAdapter;
import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;
import com.jostein.maratontrener.models.FriendProfile;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;

public class BuddiesFragment extends Fragment {

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

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_buddies, container, false);
        try {
            dbRef = FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/").getReference("profiles");
        } catch (Exception e) {
            android.util.Log.e("FirebaseInit", "Failed to initialize Firebase Realtime Database: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Firebase Init Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        workoutDao = WorkoutDatabase.getDatabase(requireContext()).workoutDao();
        editBuddyId = view.findViewById(R.id.editBuddyId);
        textYourId = view.findViewById(R.id.textYourId);
        btnAddBuddy = view.findViewById(R.id.btnAddBuddy);
        btnShareId = view.findViewById(R.id.btnShareId);
        recyclerBuddies = view.findViewById(R.id.recyclerBuddies);
        recyclerBuddies.setLayoutManager(new LinearLayoutManager(requireContext()));
        friendAdapter = new FriendAdapter();
        recyclerBuddies.setAdapter(friendAdapter);
        loadMyId(); loadFollowedBuddies(); startListeningForFollowers(); saveAndUploadProfile();
        textYourId.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Buddy ID", myId));
            Toast.makeText(requireContext(), R.string.id_copied, Toast.LENGTH_SHORT).show();
        });
        btnAddBuddy.setOnClickListener(v -> trackBuddy());
        btnShareId.setOnClickListener(v -> shareMyId());
        view.findViewById(R.id.imageProfileBuddy).setOnClickListener(v -> {
            if (getActivity() instanceof MainContainerActivity) ((MainContainerActivity) getActivity()).switchToTab(R.id.nav_profile);
        });
        return view;
    }

    @Override
    public void onResume() { super.onResume(); loadProfileImage(); }

    private void loadProfileImage() {
        try {
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext());
            String path = prefs.getString("profileImagePath", null);
            ShapeableImageView img = getView().findViewById(R.id.imageProfileBuddy);
            if (path != null && new File(path).exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options(); options.inSampleSize = 4;
                Bitmap b = BitmapFactory.decodeFile(path, options);
                if (b != null) { img.setImageBitmap(b); img.setPadding(0, 0, 0, 0); img.setImageTintList(null); img.setColorFilter(null); }
            } else {
                img.setImageResource(R.drawable.ic_person);
                img.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
                img.setPadding(spToPx(4), spToPx(4), spToPx(4), spToPx(4));
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private int spToPx(int sp) { return (int) (sp * getResources().getDisplayMetrics().scaledDensity); }

    private void shareMyId() {
        SharedPreferences userPrefs = SecurityUtils.getEncryptedPrefs(requireContext());
        String nickname = userPrefs.getString("userNickname", "Runner");
        String shareBody = getString(R.string.share_body_template, myId, nickname);
        Intent intent = new Intent(Intent.ACTION_SEND); intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_subject)); intent.putExtra(Intent.EXTRA_TEXT, shareBody);
        try {
            int qrResId = getResources().getIdentifier("qr_code", "drawable", requireActivity().getPackageName());
            if (qrResId != 0) {
                File cachePath = new File(requireContext().getCacheDir(), "images"); cachePath.mkdirs();
                File file = new File(cachePath, "qr_invite.png"); FileOutputStream stream = new FileOutputStream(file);
                Bitmap bitmap = BitmapFactory.decodeResource(getResources(), qrResId);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream); stream.close();
                Uri contentUri = FileProvider.getUriForFile(requireContext(), requireActivity().getPackageName() + ".fileprovider", file);
                intent.putExtra(Intent.EXTRA_STREAM, contentUri); intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } else intent.setType("text/plain");
        } catch (Exception e) { intent.setType("text/plain"); }
        startActivity(Intent.createChooser(intent, getString(R.string.invite_via)));
    }

    private void loadMyId() { SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext(), "BuddyPrefs"); myId = prefs.getString("my_id", "CH020721"); textYourId.setText(myId); }

    private void saveAndUploadProfile() {
        WorkoutUtils.uploadWorkoutsToFirebase(requireContext());
    }

    private void trackBuddy() {
        String buddyId = editBuddyId.getText().toString().trim().toUpperCase(); if (buddyId.isEmpty() || followedIds.contains(buddyId)) return;
        followedIds.add(buddyId); saveFollowedIds(); startListeningToBuddy(buddyId);
        if (dbRef != null && myId != null) dbRef.child(buddyId).child("followers").child(myId).setValue(true);
        editBuddyId.setText("");
    }

    private void startListeningForFollowers() {
        if (dbRef == null || myId == null) return;
        dbRef.child(myId).child("followers").addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    boolean changed = false;
                    for (DataSnapshot child : snapshot.getChildren()) {
                        String fId = child.getKey();
                        if (fId != null && !followedIds.contains(fId)) { followedIds.add(fId); startListeningToBuddy(fId); changed = true; }
                    }
                    if (changed) { saveFollowedIds(); Toast.makeText(requireContext(), "New training buddy added!", Toast.LENGTH_SHORT).show(); }
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void startListeningToBuddy(String buddyId) {
        if (dbRef == null) return;
        dbRef.child(buddyId).addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    FriendProfile profile = snapshot.getValue(FriendProfile.class);
                    if (profile != null) { profile.id = snapshot.getKey(); updateFriendInList(profile); }
                }
            }
            @Override public void onCancelled(DatabaseError error) {}
        });
    }

    private void updateFriendInList(FriendProfile updatedProfile) {
        boolean found = false;
        for (int i = 0; i < friendProfiles.size(); i++) { if (friendProfiles.get(i).id != null && friendProfiles.get(i).id.equals(updatedProfile.id)) { friendProfiles.set(i, updatedProfile); found = true; break; } }
        if (!found) friendProfiles.add(updatedProfile);
        if (getActivity() != null) getActivity().runOnUiThread(() -> friendAdapter.setFriends(new ArrayList<>(friendProfiles)));
    }

    private void loadFollowedBuddies() { SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext(), "BuddyPrefs"); Set<String> savedIds = prefs.getStringSet("followed_ids", new HashSet<>()); followedIds = new HashSet<>(savedIds); for (String id : followedIds) startListeningToBuddy(id); }

    private void saveFollowedIds() { SecurityUtils.getEncryptedPrefs(requireContext(), "BuddyPrefs").edit().putStringSet("followed_ids", followedIds).apply(); }
}