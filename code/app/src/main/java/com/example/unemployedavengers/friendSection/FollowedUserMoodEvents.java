/*
 * FollowedUserMoodEvents - Fragment for displaying mood events from followed users
 *
 * Purpose:
 * - Shows a list of mood events from users the current user is following
 * - Fetches mood event data from Firestore
 * - Handles UI display and interactions
 */
package com.example.unemployedavengers.friendSection;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.unemployedavengers.R;
import com.example.unemployedavengers.arrayadapters.FollowedUserMoodEventAdapter;
import com.example.unemployedavengers.databinding.FollowedUserMoodEventsBinding;
import com.example.unemployedavengers.models.MoodEvent;
import com.example.unemployedavengers.models.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FollowedUserMoodEvents extends Fragment {
    private FollowedUserMoodEventsBinding binding;
    private FirebaseFirestore db;
    private String currentUserId;
    private ArrayList<MoodEvent> followedUserMoodEvents;
    private FollowedUserMoodEventAdapter moodAdapter;
    private Map<String, String> userIdToUsernameMap;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FollowedUserMoodEventsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize FirebaseFirestore
        db = FirebaseFirestore.getInstance();
        userIdToUsernameMap = new HashMap<>();

        // Get current user ID from SharedPreferences
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("userID", null);

        // Initialize mood events list and adapter
        followedUserMoodEvents = new ArrayList<>();
        moodAdapter = new FollowedUserMoodEventAdapter(getContext(), followedUserMoodEvents);
        binding.followedUsersListView.setAdapter(moodAdapter);

        // Update UI title
        binding.tvFriendsMoodTitle.setText("Following Mood History");

        // Load mood events from followed users
        loadFollowedUsers();

        // Setup buttons
        binding.addFriendButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_followedUserMoodEventsFragment_to_userSearchFragment)
        );

        binding.mapButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Map view coming soon", Toast.LENGTH_SHORT).show();
        });

        binding.filterButton.setOnClickListener(v -> {
            Toast.makeText(getContext(), "Filter functionality coming soon", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * Loads the list of users that the current user follows
     */
    private void loadFollowedUsers() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.emptyStateMessage.setVisibility(View.GONE);

        if (currentUserId == null) {
            binding.progressBar.setVisibility(View.GONE);
            binding.emptyStateMessage.setText("Please log in to view following");
            binding.emptyStateMessage.setVisibility(View.VISIBLE);
            return;
        }

        db.collection("users")
                .document(currentUserId)
                .collection("following")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<String> followedUserIds = new ArrayList<>();

                    for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                        String followedId = document.getString("followedId");
                        if (followedId != null) {
                            followedUserIds.add(followedId);
                        }
                    }

                    if (followedUserIds.isEmpty()) {
                        binding.progressBar.setVisibility(View.GONE);
                        binding.emptyStateMessage.setText("You're not following anyone yet");
                        binding.emptyStateMessage.setVisibility(View.VISIBLE);
                        binding.followedUsersListView.setVisibility(View.GONE);
                    } else {
                        // Load usernames for all followed users
                        loadUsernames(followedUserIds);
                    }
                })
                .addOnFailureListener(e -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.emptyStateMessage.setText("Error loading following data");
                    binding.emptyStateMessage.setVisibility(View.VISIBLE);
                    Toast.makeText(getContext(), "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Loads usernames for all followed users
     * @param userIds List of user IDs to load usernames for
     */
    private void loadUsernames(List<String> userIds) {
        int[] completedCount = {0}; // Use array to allow modification in lambda

        for (String userId : userIds) {
            db.collection("users")
                    .document(userId)
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        User user = documentSnapshot.toObject(User.class);
                        if (user != null && user.getUsername() != null) {
                            // Store username mapping
                            userIdToUsernameMap.put(userId, user.getUsername());
                        }

                        completedCount[0]++;

                        // When all usernames are loaded, load the mood events
                        if (completedCount[0] >= userIds.size()) {
                            loadMoodEvents(userIds);
                        }
                    })
                    .addOnFailureListener(e -> {
                        completedCount[0]++;

                        // Continue loading even if some usernames fail
                        if (completedCount[0] >= userIds.size()) {
                            loadMoodEvents(userIds);
                        }
                    });
        }
    }

    /**
     * Loads mood events from all followed users
     * @param userIds List of user IDs to load mood events for
     */
    private void loadMoodEvents(List<String> userIds) {
        followedUserMoodEvents.clear();
        int[] completedCount = {0}; // Use array to allow modification in lambda

        for (String userId : userIds) {
            db.collection("users")
                    .document(userId)
                    .collection("moods")
                    .get()
                    .addOnSuccessListener(querySnapshot -> {
                        for (QueryDocumentSnapshot doc : querySnapshot) {
                            MoodEvent moodEvent = doc.toObject(MoodEvent.class);

                            // Set the user ID so we can display the username
                            moodEvent.setUserId(userId); // Assuming you've added this field to MoodEvent

                            followedUserMoodEvents.add(moodEvent);
                        }

                        completedCount[0]++;

                        // When all mood events are loaded, update the UI
                        if (completedCount[0] >= userIds.size()) {
                            updateUI();
                        }
                    })
                    .addOnFailureListener(e -> {
                        completedCount[0]++;

                        // Continue updating UI even if some mood events fail to load
                        if (completedCount[0] >= userIds.size()) {
                            updateUI();
                        }
                    });
        }
    }

    /**
     * Updates the UI with loaded mood events
     */
    private void updateUI() {
        binding.progressBar.setVisibility(View.GONE);

        if (followedUserMoodEvents.isEmpty()) {
            binding.emptyStateMessage.setText("No mood events from followed users");
            binding.emptyStateMessage.setVisibility(View.VISIBLE);
            binding.followedUsersListView.setVisibility(View.GONE);
        } else {
            // Sort mood events by time (newest first)
            Collections.sort(followedUserMoodEvents,
                    (e1, e2) -> Long.compare(e2.getTime(), e1.getTime()));

            // Update the adapter with username mappings
            moodAdapter.setUserIdToUsernameMap(userIdToUsernameMap);
            moodAdapter.notifyDataSetChanged();

            binding.emptyStateMessage.setVisibility(View.GONE);
            binding.followedUsersListView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}