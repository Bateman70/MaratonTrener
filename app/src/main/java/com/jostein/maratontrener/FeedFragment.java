package com.jostein.maratontrener;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
import com.jostein.maratontrener.models.FriendProfile;

import com.jostein.maratontrener.database.WorkoutDao;
import com.jostein.maratontrener.database.WorkoutDatabase;
import com.jostein.maratontrener.database.WorkoutEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

public class FeedFragment extends Fragment {

    private RecyclerView recyclerFeed;
    private View emptyStateFeed;
    private ShapeableImageView imageProfileFeed;
    private FriendAdapter friendAdapter;
    private List<FriendProfile> friendProfiles = new ArrayList<>();
    private DatabaseReference dbRef;
    private Set<String> followedIds = new HashSet<>();
    private String myId;
    private WorkoutDao workoutDao;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        recyclerFeed = view.findViewById(R.id.recyclerFeed);
        emptyStateFeed = view.findViewById(R.id.emptyStateFeed);
        imageProfileFeed = view.findViewById(R.id.imageProfileFeed);

        recyclerFeed.setLayoutManager(new LinearLayoutManager(getContext()));
        friendAdapter = new FriendAdapter();
        recyclerFeed.setAdapter(friendAdapter);

        workoutDao = WorkoutDatabase.getDatabase(requireContext()).workoutDao();

        try {
            dbRef = FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/").getReference("profiles");
        } catch (Exception e) {
            android.util.Log.e("FirebaseInit", "Failed to initialize Firebase Realtime Database inside Feed: " + e.getMessage(), e);
            Toast.makeText(requireContext(), "Firebase Init Error (Feed): " + e.getMessage(), Toast.LENGTH_LONG).show();
        }

        view.findViewById(R.id.btnManageBuddies).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), BuddyActivity.class));
        });

        view.findViewById(R.id.btnListBuddies).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), BuddyActivity.class));
        });

        view.findViewById(R.id.btnEmptyAddBuddy).setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), BuddyActivity.class));
        });

        imageProfileFeed.setOnClickListener(v -> {
            if (getActivity() instanceof MainContainerActivity) {
                ((MainContainerActivity) getActivity()).switchToTab(R.id.nav_profile);
            } else {
                startActivity(new Intent(getActivity(), ProfileActivity.class));
            }
        });

        loadMyId();
        loadFollowedBuddies();
        startListeningForFollowers();
        loadProfileImage();
        saveAndUploadProfile();
        return view;
    }

    private void startListeningForFollowers() {
        if (dbRef == null || myId == null) return;
        
        dbRef.child(myId).child("followers").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
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
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveFollowedIds() {
        if (getContext() != null) {
            SecurityUtils.getEncryptedPrefs(requireContext(), "BuddyPrefs").edit()
                .putStringSet("followed_ids", followedIds)
                .apply();
        }
    }

    private void saveAndUploadProfile() {
        if (getContext() != null) {
            WorkoutUtils.uploadWorkoutsToFirebase(requireContext());
        }
    }

    private void loadMyId() {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext(), "BuddyPrefs");
        myId = prefs.getString("my_id", "CH020721");
    }

    private void loadFollowedBuddies() {
        SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext(), "BuddyPrefs");
        Set<String> savedIds = prefs.getStringSet("followed_ids", new HashSet<>());
        followedIds = new HashSet<>(savedIds);
        
        if (followedIds.isEmpty()) {
            emptyStateFeed.setVisibility(View.VISIBLE);
            recyclerFeed.setVisibility(View.GONE);
        } else {
            emptyStateFeed.setVisibility(View.GONE);
            recyclerFeed.setVisibility(View.VISIBLE);
            friendProfiles.clear();
            for (String id : followedIds) {
                startListeningToBuddy(id);
            }
        }
    }

    private void startListeningToBuddy(String buddyId) {
        if (dbRef == null) return;
        dbRef.child(buddyId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    FriendProfile profile = snapshot.getValue(FriendProfile.class);
                    if (profile != null) {
                        profile.id = snapshot.getKey();
                        updateFriendInList(profile);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void updateFriendInList(FriendProfile updatedProfile) {
        boolean found = false;
        for (int i = 0; i < friendProfiles.size(); i++) {
            if (friendProfiles.get(i).id != null && friendProfiles.get(i).id.equals(updatedProfile.id)) {
                friendProfiles.set(i, updatedProfile);
                found = true;
                break;
            }
        }
        if (!found) {
            friendProfiles.add(updatedProfile);
        }
        
        if (isAdded()) {
            requireActivity().runOnUiThread(() -> {
                friendAdapter.setFriends(new ArrayList<>(friendProfiles));
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProfileImage();
        loadFollowedBuddies();
    }

    private void loadProfileImage() {
        try {
            SharedPreferences prefs = SecurityUtils.getEncryptedPrefs(requireContext());
            String path = prefs.getString("profileImagePath", null);
            if (path != null && new File(path).exists()) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = 4;
                Bitmap b = BitmapFactory.decodeFile(path, options);
                if (b != null) {
                    imageProfileFeed.setImageBitmap(b);
                    imageProfileFeed.setPadding(0, 0, 0, 0);
                    imageProfileFeed.setImageTintList(null);
                    imageProfileFeed.setColorFilter(null);
                }
            } else {
                imageProfileFeed.setImageResource(R.drawable.ic_person);
                imageProfileFeed.setImageTintList(android.content.res.ColorStateList.valueOf(getResources().getColor(R.color.electric_lime)));
                imageProfileFeed.setPadding(spToPx(4), spToPx(4), spToPx(4), spToPx(4));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }
}