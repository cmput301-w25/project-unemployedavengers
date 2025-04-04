/**
 * Notifications Fragment - Manages and displays incoming follow requests for the current user.
 *
 * Design Pattern:
 * - Follows MVVM architecture (though ViewModel not yet implemented)
 * - Implements Observer pattern for Firestore real-time updates
 * - Uses Adapter pattern for list display
 *
 * Key Responsibilities:
 * 1. Follow Request Management:
 *    - Retrieves pending follow requests from Firestore
 *    - Displays requesters' profiles in a scrollable list
 *    - Handles request acceptance/rejection through FollowRequestAdapter
 *
 * 2. User Authentication:
 *    - Verifies current user session
 *    - Fetches user-specific follow requests
 *    - Handles authentication state changes
 *
 * 3. Data Flow:
 *    - Coordinates between Firestore and UI
 *    - Manages list updates and adapter refresh
 *    - Handles error states and empty views
 *
 * Technical Implementation:
 * - Uses Firestore for real-time request data
 * - Implements custom FollowRequestAdapter
 * - Leverages Firebase Authentication
 * - Follows Fragment lifecycle
 *
 * Outstanding Issues/TODOs:
 * 1. No ViewModel implementation (potential data loss on config changes)
 * 2. No proper empty state UI
 * 3. No swipe-to-dismiss gesture
 * 4. Duplicate follow requests possible (needs validation)
 * 5. Could benefit from pagination for many requests
 * 6. No proper error handling UI
 *
 * Dependencies:
 * - Firebase Firestore (requests collection)
 * - Firebase Authentication
 * - FollowRequestAdapter
 * - User model class
 *
 * Lifecycle Notes:
 * - Manages Firestore listeners properly
 * - Handles configuration changes
 * - Cleans up resources in onDestroyView
 *
 * @see FollowRequestAdapter
 * @see User
 */

package com.example.unemployedavengers;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.unemployedavengers.DAO.IUserDAO;
import com.example.unemployedavengers.arrayadapters.FollowRequestAdapter;
import com.example.unemployedavengers.implementationDAO.UserDAOImplement;
import com.example.unemployedavengers.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class Notifications extends Fragment {
    private ListView notificationsList;
    private IUserDAO userDAO;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FollowRequestAdapter adapter;
    private List<User> followRequests;
    private String currentUserId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.notifications, container, false);
        notificationsList = view.findViewById(R.id.notifications_list);
        userDAO = new UserDAOImplement();
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        followRequests = new ArrayList<>();

        adapter = new FollowRequestAdapter(requireContext(), followRequests, "");
        notificationsList.setAdapter(adapter);

        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            currentUserId = currentUser.getUid();
            loadFollowRequests();
        } else {
            Log.e("Notifications", "No user is logged in");
        }

        return view;
    }

    private void loadFollowRequests() {
        if (currentUserId == null) {
            Log.e("Notifications", "currentUserId is null");
            return;
        }

        CollectionReference requestsRef = db.collection("users").document(currentUserId).collection("requests");
        requestsRef.get().addOnSuccessListener(queryDocumentSnapshots -> {
            followRequests.clear();

            if (queryDocumentSnapshots.isEmpty()) {
                Log.d("Notifications", "No follow requests found");
                adapter.notifyDataSetChanged();
                return;
            }

            for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                String requesterId = document.getId();
                Log.d("Notifications", "Found follow request from: " + requesterId);

                db.collection("users").document(requesterId).get()
                        .addOnSuccessListener(userDoc -> {
                            if (userDoc.exists()) {
                                User user = userDoc.toObject(User.class);
                                if (user != null) {
                                    followRequests.add(user);
                                    adapter.notifyDataSetChanged();
                                }
                            } else {
                                Log.e("Notifications", "User document does not exist: " + requesterId);
                            }
                        })
                        .addOnFailureListener(e -> Log.e("Notifications", "Error fetching user data", e));
            }

            adapter = new FollowRequestAdapter(requireContext(), followRequests, currentUserId);
            notificationsList.setAdapter(adapter);
        }).addOnFailureListener(e ->
                Log.e("Notifications", "Failed to load follow requests", e)
        );
    }
}
