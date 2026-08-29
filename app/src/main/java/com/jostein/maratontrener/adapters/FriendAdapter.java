package com.jostein.maratontrener.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.jostein.maratontrener.R;
import com.jostein.maratontrener.models.FriendProfile;
import android.content.SharedPreferences;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.jostein.maratontrener.SecurityUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.FriendViewHolder> {

    private List<FriendProfile> friends = new ArrayList<>();

    public void setFriends(List<FriendProfile> friends) {
        this.friends = friends;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        FriendProfile profile = friends.get(position);
        holder.textName.setText(profile.name);
        holder.textIdDisplay.setText("#" + (profile.id != null ? profile.id : "DEMO"));
        holder.textDistance.setText(String.format(Locale.getDefault(), "%.1f km", profile.distance));
        holder.textConsistency.setText(profile.consistency + "%");
        holder.textWorkouts.setText(String.format(Locale.getDefault(), "%d / %d Workouts Completed", 
                profile.workoutsDone, profile.workoutsTotal));
        
        if (profile.currentRace != null && !profile.currentRace.isEmpty()) {
            holder.textRace.setText("Training for: " + profile.currentRace);
            holder.textRace.setVisibility(View.VISIBLE);
        } else {
            holder.textRace.setVisibility(View.GONE);
        }

        long diff = System.currentTimeMillis() - profile.lastUpdate;
        String timeAgo;
        if (diff < 3600000) timeAgo = (diff / 60000) + "m ago";
        else if (diff < 86400000) timeAgo = (diff / 3600000) + "h ago";
        else timeAgo = (diff / 86400000) + "d ago";
        holder.textLastUpdate.setText(timeAgo);

        SharedPreferences buddyPrefs = SecurityUtils.getEncryptedPrefs(holder.itemView.getContext(), "BuddyPrefs");
        String myId = buddyPrefs.getString("my_id", "CH020721");

        boolean hasHighFive = profile.hasHighFiveFrom(myId);
        int count = profile.getHighFiveCount();

        if (hasHighFive) {
            holder.btnHighFive.setText("🙌 HIGH-FIVED (" + count + ")");
            holder.btnHighFive.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.electric_lime));
        } else {
            holder.btnHighFive.setText(count > 0 ? "🙌 HIGH-FIVE (" + count + ")" : "🙌 HIGH-FIVE");
            holder.btnHighFive.setTextColor(android.graphics.Color.parseColor("#88ffffff")); // light gray/dim white
        }

        holder.btnHighFive.setOnClickListener(v -> {
            if (profile.id == null) return;

            SharedPreferences userPrefs = SecurityUtils.getEncryptedPrefs(v.getContext());
            String myNickname = userPrefs.getString("userNickname", userPrefs.getString("userName", "A buddy"));

            DatabaseReference highFiveRef = FirebaseDatabase.getInstance("https://the-rum-runner-default-rtdb.europe-west1.firebasedatabase.app/")
                    .getReference("profiles")
                    .child(profile.id)
                    .child("highFives")
                    .child(myId);

            if (hasHighFive) {
                highFiveRef.removeValue();
                Toast.makeText(v.getContext(), "Removed High-Five for " + profile.name, Toast.LENGTH_SHORT).show();
            } else {
                highFiveRef.setValue(myNickname);
                Toast.makeText(v.getContext(), "Sent High-Five to " + profile.name + "!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    static class FriendViewHolder extends RecyclerView.ViewHolder {
        TextView textName, textIdDisplay, textDistance, textConsistency, textWorkouts, textLastUpdate, textRace;
        Button btnHighFive;

        public FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.textFriendName);
            textIdDisplay = itemView.findViewById(R.id.textFriendIdDisplay);
            textDistance = itemView.findViewById(R.id.textFriendDistance);
            textConsistency = itemView.findViewById(R.id.textFriendConsistency);
            textWorkouts = itemView.findViewById(R.id.textFriendWorkouts);
            textLastUpdate = itemView.findViewById(R.id.textLastUpdate);
            textRace = itemView.findViewById(R.id.textFriendRace);
            btnHighFive = itemView.findViewById(R.id.btnHighFive);
        }
    }
}