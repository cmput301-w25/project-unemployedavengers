package com.example.unemployedavengers.friendSection;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.unemployedavengers.R;
import com.example.unemployedavengers.databinding.FriendsHistoryBinding;
import com.example.unemployedavengers.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FriendsHistory extends Fragment {
    private FriendsHistoryBinding binding;
    private FirebaseFirestore db;
    private String currentUserId;
    private List<User> followedUsers;
    private ArrayAdapter<String> friendsAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FriendsHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize Firestore
        db = FirebaseFirestore.getInstance();

        // Get current user ID from SharedPreferences
        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        currentUserId = sharedPreferences.getString("userID", null);

        if (currentUserId == null) {
            Toast.makeText(getContext(), "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Initialize the list and adapter
        followedUsers = new ArrayList<>();
        friendsAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                new ArrayList<>()
        );
        binding.friendsHistoryList.setAdapter(friendsAdapter);

        // Load friends list
        loadFriendsList();

        // Setup add friend button
        binding.addFriendButton.setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_friendsHistoryFragment_to_userSearchFragment)
        );
    }

    private void loadFriendsList() {
        db.collection("users")
                .document(currentUserId)
                .collection("following")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            updateUIForEmptyFriendsList();
                            return;
                        }

                        List<String> followedUserIds = new ArrayList<>();
                        for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                            String followedUserId = doc.getString("followedId");
                            if (followedUserId != null) {
                                followedUserIds.add(followedUserId);
                            }
                        }

                        // Only fetch details if we have followed user IDs
                        if (!followedUserIds.isEmpty()) {
                            fetchFollowedUsersDetails(followedUserIds);
                        } else {
                            updateUIForEmptyFriendsList();
                        }
                    } else {
                        updateUIForEmptyFriendsList();
                    }
                });
    }

    private void fetchFollowedUsersDetails(List<String> followedUserIds) {
        List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

        for (String userId : followedUserIds) {
            Task<DocumentSnapshot> task = db.collection("users")
                    .document(userId)
                    .get();
            tasks.add(task);
        }

        Tasks.whenAllComplete(tasks)
                .addOnSuccessListener(taskResults -> {
                    followedUsers.clear();
                    friendsAdapter.clear();

                    for (Object resultTask : taskResults) {
                        Task<DocumentSnapshot> task = (Task<DocumentSnapshot>) resultTask;
                        try {
                            if (task.isSuccessful()) {
                                DocumentSnapshot documentSnapshot = task.getResult();

                                if (documentSnapshot != null && documentSnapshot.exists()) {
                                    User user = documentSnapshot.toObject(User.class);

                                    if (user != null) {
                                        followedUsers.add(user);
                                        friendsAdapter.add(user.getUsername());
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Handle exception silently in production
                        }
                    }

                    // Update UI
                    if (followedUsers.isEmpty()) {
                        updateUIForEmptyFriendsList();
                    } else {
                        updateUIWithFriendsList();
                    }
                })
                .addOnFailureListener(e -> {
                    updateUIForEmptyFriendsList();
                });
    }

    private void updateUIForEmptyFriendsList() {
        friendsAdapter.clear();
        Toast.makeText(getContext(), "You haven't followed any users yet", Toast.LENGTH_SHORT).show();
    }

    private void updateUIWithFriendsList() {
        friendsAdapter.notifyDataSetChanged();

        // Set click listener to view friend's mood events
        binding.friendsHistoryList.setOnItemClickListener((parent, view, position, id) -> {
            User selectedUser = followedUsers.get(position);

            // Create a bundle to pass the selected user's information
            Bundle bundle = new Bundle();
            bundle.putString("followedUserId", selectedUser.getUserId());
            bundle.putString("followedUsername", selectedUser.getUsername());

            // Navigate to the followed user's mood events fragment
            Navigation.findNavController(view)
                    .navigate(R.id.action_friendsHistoryFragment_to_followedUserMoodEventsFragment, bundle);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}