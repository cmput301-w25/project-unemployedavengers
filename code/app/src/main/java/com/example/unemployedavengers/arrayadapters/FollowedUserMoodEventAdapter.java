/*
 * FollowedUserMoodEventAdapter - A custom adapter for displaying mood events from followed users.
 *
 * Purpose:
 * - Extends ArrayAdapter to display MoodEvent objects in a ListView with custom formatting
 * - Shows the username of the followed user alongside their mood information
 * - Applies appropriate color styling based on the mood type
 * - Loads and displays mood images if available
 */
package com.example.unemployedavengers.arrayadapters;

import android.content.Context;
import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.unemployedavengers.R;
import com.example.unemployedavengers.models.MoodEvent;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FollowedUserMoodEventAdapter extends ArrayAdapter<MoodEvent> {

    private Context context;
    private Map<String, String> userIdToUsernameMap;

    public FollowedUserMoodEventAdapter(Context context, List<MoodEvent> moodEvents) {
        super(context, 0, moodEvents);
        this.context = context;
        this.userIdToUsernameMap = new HashMap<>();
    }

    /**
     * Sets the username map to associate user IDs with usernames
     * @param userIdToUsernameMap Map of user IDs to usernames
     */
    public void setUserIdToUsernameMap(Map<String, String> userIdToUsernameMap) {
        this.userIdToUsernameMap = userIdToUsernameMap;
    }

    /**
     * Adds a single username mapping to the map
     * @param userId The user ID
     * @param username The username
     */
    public void addUsernameMapping(String userId, String username) {
        if (userId != null && username != null) {
            this.userIdToUsernameMap.put(userId, username);
        }
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.followed_user_mood_items, parent, false);
        }

        MoodEvent moodEvent = getItem(position);
        if (moodEvent == null) return view;

        // Get references to views
        TextView usernameText = view.findViewById(R.id.usernameText);
        TextView moodText = view.findViewById(R.id.mood_text);
        TextView dateText = view.findViewById(R.id.date_text);
        TextView reasonText = view.findViewById(R.id.reasonText);
        TextView situationText = view.findViewById(R.id.situationText);
        ImageView moodImageView = view.findViewById(R.id.moodImageView);

        // Set the username - use the mapping or a default value
        // Note: You'll need to ensure the mapping is populated with usernames
        // The MoodEvent should have a userId field or other way to identify the owner
        String userId = moodEvent.getUserId(); // Assuming MoodEvent has a getUserId method
        String username = "Unknown User";

        if (userId != null && userIdToUsernameMap.containsKey(userId)) {
            username = userIdToUsernameMap.get(userId);
        }

        usernameText.setText(username);

        // Set the mood text and apply color
        moodText.setText(moodEvent.getMood());
        moodText.setTextColor(getMoodColor(getContext(), moodEvent.getMood()));

        // Format and set the date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(new Date(moodEvent.getTime()));
        dateText.setText(formattedTime);

        // Set reason and situation texts
        String reason = moodEvent.getReason();
        reasonText.setText(TextUtils.isEmpty(reason) ? "None" : reason);

        String situation;
        if (!TextUtils.isEmpty(moodEvent.getRadioSituation()) &&
                !moodEvent.getRadioSituation().equals("Not Set") &&
                !moodEvent.getRadioSituation().equals("None")) {
            situation = moodEvent.getRadioSituation();
        } else if (!TextUtils.isEmpty(moodEvent.getSituation())) {
            situation = moodEvent.getSituation();
        } else {
            situation = "Not specified";
        }
        situationText.setText(situation);

        // Load and display the image if available
        if (moodEvent.getImageUri() != null && !moodEvent.getImageUri().isEmpty()) {
            moodImageView.setVisibility(View.VISIBLE);
            Glide.with(context)
                    .load(moodEvent.getImageUri())
                    .into(moodImageView);
        } else {
            moodImageView.setVisibility(View.GONE);
        }

        return view;
    }

    // Method to return a color based on mood
    private int getMoodColor(Context context, String mood) {
        String lowerMood = mood.toLowerCase(); // Normalize case

        if (lowerMood.contains("anger")) return Color.RED;
        if (lowerMood.contains("confusion")) return ContextCompat.getColor(context, R.color.orange);
        if (lowerMood.contains("disgust")) return Color.GREEN;
        if (lowerMood.contains("fear")) return Color.BLUE;
        if (lowerMood.contains("happiness")) return ContextCompat.getColor(context, R.color.baby_blue);
        if (lowerMood.contains("sadness")) return Color.GRAY;
        if (lowerMood.contains("shame")) return ContextCompat.getColor(context, R.color.yellow);
        if (lowerMood.contains("surprise")) return ContextCompat.getColor(context, R.color.pink);

        return ContextCompat.getColor(context, R.color.black); // Default color
    }
}