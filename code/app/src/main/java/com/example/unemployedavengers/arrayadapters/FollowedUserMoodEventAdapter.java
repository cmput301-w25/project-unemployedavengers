/**
 * FollowedUserMoodEventAdapter - A custom adapter for displaying mood events from followed users.
 *
 * Purpose:
 * - Extends `ArrayAdapter` to display `MoodEvent` objects in a `ListView` with custom formatting.
 * - Displays the username of the followed user alongside their mood information (mood type and timestamp).
 * - Dynamically fetches usernames and profile pictures from Firebase Firestore if not available locally.
 * - Applies appropriate color styling to mood text based on mood type.
 * - Formats and displays the timestamp of each mood event.
 *
 * Design Pattern:
 * - Implements the `ArrayAdapter` design pattern to efficiently manage a list of `MoodEvent` objects and display them in a `ListView`.
 * - Fetches user information (username and profile picture) dynamically from Firestore in real-time, ensuring up-to-date data for each followed user.
 * - Supports dynamic updates by modifying the `userIdToUsernameMap` to allow the association of user IDs with usernames.
 * - Uses the `Glide` library to efficiently load and display user profile pictures from a URL.
 *
 * Outstanding Issues:
 * - The asynchronous loading of usernames and profile pictures from Firestore might lead to delays in displaying complete information, causing a potential lag in rendering the `ListView`.
 * - The use of `getView` for database access in the UI thread could cause performance bottlenecks, especially with a large number of `MoodEvent` objects.
 * - Error handling is minimal: If Firestore retrieval fails, it defaults to the username "Unknown User", but further feedback or fallbacks might be needed to improve the user experience.
 * - The code could be optimized to avoid fetching user data multiple times if the user already exists in the `userIdToUsernameMap`.
 */

package com.example.unemployedavengers.arrayadapters;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
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
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

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
        TextView moodText = view.findViewById(R.id.mood_text);
        TextView dateText = view.findViewById(R.id.date_text);
        TextView usernameText = view.findViewById(R.id.usernameText);


        // Set the mood text and apply color
        moodText.setText(moodEvent.getMood());
        moodText.setTextColor(getMoodColor(getContext(), moodEvent.getMood()));

        // Format and set the date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        String formattedTime = sdf.format(new Date(moodEvent.getTime()));
        dateText.setText(formattedTime);

        // Set username
        String userId = moodEvent.getUserId();
        final String[] username = new String[]{"Unknown User"}; // Use array to allow modification within listener

        if (userId != null) {

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            DocumentReference userDocRef = db.collection("users").document(userId);

            ImageView image = (ImageView) view.findViewById(R.id.profileIcon);

            userDocRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String profilePicUrl = documentSnapshot.getString("avatar");
                    if (profilePicUrl != null && !profilePicUrl.isEmpty()) {
                        Glide.with(context).load(profilePicUrl).into(image);
                    }
                }
            }).addOnFailureListener(e -> {
                Log.e("CommentAdapter", "Failed to load profile picture", e);
            });

            userDocRef.get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                // Get the username from the document
                                String fetchedUsername = documentSnapshot.getString("username");
                                if (fetchedUsername != null) {
                                    username[0] = fetchedUsername;
                                } else {
                                    Log.d("GetUserID", "Username not found in document");
                                }
                            } else {
                                Log.d("GetUserID", "No document found for userId: " + userId);
                            }
                            usernameText.setText(username[0]);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(Exception e) {
                            Log.d("GetUserID", "Error getting document: " + e.getMessage());
                            // On failure, set the default username
                            usernameText.setText(username[0]);
                        }
                    });
        } else {
            usernameText.setText(username[0]);
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