/*
 * Dashboard Fragment
 * --------------------
 * Purpose:
 * - Display the main dashboard of the Unemployed Avengers app.
 * - Load and show mood events from Firestore using MoodEventArrayAdapter.
 * - Provide navigation to other screens (e.g., friends history, profile, input dialog).
 * - Handle user interactions such as selecting mood events, editing, and deleting.
 */
package com.example.unemployedavengers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentResultListener;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.example.unemployedavengers.DAO.IUserDAO;
import com.example.unemployedavengers.arrayadapters.MoodEventArrayAdapter;
import com.example.unemployedavengers.databinding.DashboardBinding;
import com.example.unemployedavengers.implementationDAO.UserDAOImplement;
import com.example.unemployedavengers.models.FriendMoodEventsViewModel;
import com.example.unemployedavengers.models.MoodEvent;
import com.example.unemployedavengers.models.MoodEventsViewModel;
import com.example.unemployedavengers.models.WithinFiveKmViewModel;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.maps.android.SphericalUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Dashboard extends BaseFragment {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private FusedLocationProviderClient fusedLocationClient;
    private DashboardBinding binding;
    private ArrayList<MoodEvent> moodList;
    private MoodEventArrayAdapter moodAdapter;
    private FirebaseFirestore db;
    private CollectionReference moodEventRef;
    private IUserDAO userDAO;
    private String userID;
    private String username;

    private double currentLatitude;
    private double currentLongitude;

    private MoodEvent selectedMoodForDeletion;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = DashboardBinding.inflate(inflater, container, false);
        return binding.getRoot();

        // deviceID = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        userDAO = new UserDAOImplement();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        // Initialize FirebaseFirestore
        db = FirebaseFirestore.getInstance();

        // Configure offline persistence if needed
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);


        if (!isValidFragment()) return;

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        // Add this utility method to check network connectivity requireActivity().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);

        // Check if user is logged in via SharedPreferences
        boolean isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false);
        // Retrieve values
        username = sharedPreferences.getString("username", null);  // Default to null if not found
        userID = sharedPreferences.getString("userID", null);  // Default to null if not found

        // Check network connectivity
        boolean isOnline = isNetworkAvailable();
        // Handle authentication based on online/offline status
        if (isOnline) {
            // Online mode: Use Firebase Auth
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                // Redirect to login if not authenticated through Firebase
                Navigation.findNavController(view).navigate(R.id.action_dashboardFragment_to_loginFragment);
                return;
            }
            userID = currentUser.getUid();
        } else {
            // Offline mode: Use cached credentials
            if (!isLoggedIn || userID == null) {
                // Not logged in offline, redirect to login
                Toast.makeText(requireContext(), "Please log in. You are offline.", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(view).navigate(R.id.action_dashboardFragment_to_loginFragment);
                return;
            }
            // Continue with cached userID and username

        }

        // Only try to fetch username from Firestore if we're online and username isn't cached
        if (isOnline && (username == null)) {
            DocumentReference userDocRef = db.collection("users").document(userID);
            userDocRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    username = task.getResult().getString("username");

                    // Cache the username for offline use
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("username", username);
                    editor.apply();
                } else {
                    if (isValidFragment()) {
                        Toast.makeText(requireContext(), "User data not available", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }


        // Get user document reference
        DocumentReference userDocRef = db.collection("users").document(userID);

        // Fetch username from Firestore (works offline if previously cached)
        userDocRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                username = task.getResult().getString("username");
            } else {
                if (isValidFragment()) {
                    Toast.makeText(requireContext(), "User data not available", Toast.LENGTH_SHORT).show();
                }
            }
        });
        if (userID != null) {
            moodEventRef = db.collection("users").document(userID).collection("moods");
        } else {
            if (isValidFragment()) {
                Toast.makeText(requireContext(), "User ID not found, please login again", Toast.LENGTH_LONG).show();
            }
            return;
        }

        moodList = new ArrayList<>();
        moodAdapter = new MoodEventArrayAdapter(requireContext(), moodList);
        binding.activityList.setAdapter(moodAdapter);
        loadFollowedMoodEvents();

        //load mood event function
        loadMoodEvents();

        binding.friendsButton.setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.action_dashboardFragment_to_userSearchFragment);
        });

        //Navigates to the input dialog
        binding.addMoodButton.setOnClickListener(v -> {
            Bundle args = new Bundle();
            args.putString("source", "dashboard");
            Navigation.findNavController(v).navigate(R.id.action_dashboardFragment_to_inputDialog, args);
        });

        //register the listener for the result from InputDialog (Only once)
        getParentFragmentManager().setFragmentResultListener("input_dialog_result", getViewLifecycleOwner(), new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (binding == null || !isValidFragment()) return;

                //retrieve the MoodEvent object
                MoodEvent moodEvent = (MoodEvent) result.getSerializable("mood_event_key");
                //existed is true if we are updating a mood, else we are adding a mood
                if ((moodEvent != null) && (!moodEvent.getExisted())) {
                    addMoodEvent(moodEvent); //store the mood event in Firestore
                } else if (moodEvent != null) {
                    updateMoodEvent(moodEvent);
                }
            }
        });

        //register the result listener for the delete confirmation
        getParentFragmentManager().setFragmentResultListener("delete_mood_event", getViewLifecycleOwner(), new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                if (binding == null || !isValidFragment()) return;

                boolean deleteConfirmed = result.getBoolean("DeleteConfirmed", false);
                if (deleteConfirmed && selectedMoodForDeletion != null) {
                    //proceed with the deletion
                    onDeleteConfirmed(selectedMoodForDeletion);
                    loadMoodEvents(); //reload the mood events after deletion
                }
            }
        });
    }

    // method to check network connectivity
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) requireActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    //adds a new mood event to the database
    public void addMoodEvent(MoodEvent moodEvent) {
        if (binding == null || moodEventRef == null || !isValidFragment()) return;

        moodEvent.setExisted(true); //mark it as an existing mood event
        // Set user info from current session
        moodEvent.setUserId(userID);
        moodEvent.setUserName(username);
        moodEvent.setExisted(true);

        //add the mood event without the ID first
        moodEventRef.add(moodEvent)
                .addOnSuccessListener(documentReference -> {
                    if (binding == null || !isValidFragment()) return;

                    //get the unique ID generated by Firestore
                    String id = documentReference.getId();

                    //update the document with the ID field
                    documentReference.update("id", id)
                            .addOnSuccessListener(aVoid -> {
                                if (binding == null || !isValidFragment()) return;

                                //set the id in the local object
                                moodEvent.setId(id);
                                moodEvent.setUserId(userID);
                                moodEvent.setUserName((username));

                                //update user
                                updateMoodEvent(moodEvent);
                                Toast.makeText(getContext(), "Mood added successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                if (binding == null || !isValidFragment()) return;
                                Toast.makeText(getContext(), "Failed to update ID in Firestore", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    if (binding == null || !isValidFragment()) return;
                    Toast.makeText(getContext(), "Failed to add mood", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateMoodEvent(MoodEvent moodEvent) {
        if (binding == null || moodEventRef == null || !isValidFragment() || moodEvent == null) return;

        // NEED TO RELOAD DATABASE CAUSE FIREBASE IS AN IDIOT (crashes if you add a moodEvent and tries to update it right away cause "cannot find id")

        loadMoodEvents();

        String moodEventId = moodEvent.getId();
        if (moodEventId == null) {
            Log.e("Dashboard", "Mood event ID is null");
            return;
        }

        // Get document via id
        DocumentReference moodEventDocRef = moodEventRef.document(moodEventId);

        // Set the new values for the document
        moodEventDocRef.set(moodEvent)  //use set() to update or create the document if it doesn't exist
                .addOnSuccessListener(aVoid -> {
                    if (binding == null || !isValidFragment()) return;
                    Toast.makeText(getContext(), "Mood updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (binding == null || !isValidFragment()) return;
                    Toast.makeText(getContext(), "Failed to update mood", Toast.LENGTH_SHORT).show();
                });
    }

    //delete function
    public void onDeleteConfirmed(MoodEvent moodEvent) {
        if (binding == null || moodEventRef == null || !isValidFragment() || moodEvent == null || moodEvent.getId() == null) return;

        moodEventRef.document(moodEvent.getId()).delete() //using the id to delete
                .addOnSuccessListener(aVoid -> {
                    if (binding == null || !isValidFragment()) return;
                    Toast.makeText(getContext(), "Mood deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    if (binding == null || !isValidFragment()) return;
                    Toast.makeText(getContext(), "Failed to delete mood", Toast.LENGTH_SHORT).show();
                });

        loadMoodEvents(); //reload mood events
    }

    public void loadFollowedMoodEvents() {
        if (binding == null || !isValidFragment()) return;

        // Create an empty list to collect mood events.
        List<MoodEvent> followedEventsList = new ArrayList<>();
        List<MoodEvent> withinFiveEventsList = new ArrayList<>();
        FriendMoodEventsViewModel vm = new ViewModelProvider(requireActivity()).get(FriendMoodEventsViewModel.class);
        WithinFiveKmViewModel withinFiveKmViewModel = new ViewModelProvider(requireActivity()).get(WithinFiveKmViewModel.class);

        if (ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // Request the current location
        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    if (binding == null || !isValidFragment()) return;

                    if (location != null) {
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();
                        Toast.makeText(getContext(), "Current location set.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Unable to retrieve current location", Toast.LENGTH_SHORT).show();
                    }

                    if (binding == null || !isValidFragment() || userID == null) return;

                    LatLng currentLocation = new LatLng(currentLatitude, currentLongitude);

                    // Query the current user's "following" subcollection to get followed user IDs.
                    db.collection("users")
                            .document(userID)
                            .collection("following")
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                if (binding == null || !isValidFragment()) return;

                                List<String> followedUserIds = new ArrayList<>();

                                // Extract each followed user ID from the "following" documents.
                                for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                                    String followedId = document.getString("followedId");
                                    if (followedId != null) {
                                        followedUserIds.add(followedId);
                                    }
                                }

                                if (followedUserIds.isEmpty()) {
                                    vm.setMoodEvents(followedEventsList);
                                    return;
                                }

                                // For each followed user, query the 3 most recent mood events.
                                for (String followedId : followedUserIds) {
                                    db.collection("users")
                                            .document(followedId)
                                            .collection("moods")
                                            .orderBy("time", Query.Direction.DESCENDING) // sort by time (newest first)
                                            .limit(3)
                                            .get()
                                            .addOnSuccessListener(querySnapshot1 -> {
                                                if (binding == null || !isValidFragment()) return;

                                                for (QueryDocumentSnapshot doc : querySnapshot1) {
                                                    MoodEvent moodEvent = doc.toObject(MoodEvent.class);
                                                    if (moodEvent != null) {
                                                        followedEventsList.add(moodEvent);
                                                        if (moodEvent.getHasLocation()) {
                                                            LatLng eventLocation = new LatLng(moodEvent.getLatitude(), moodEvent.getLongitude());
                                                            double distanceInMeters = SphericalUtil.computeDistanceBetween(currentLocation, eventLocation);
                                                            if (distanceInMeters <= 5000) {
                                                                withinFiveEventsList.add(moodEvent);
                                                            }
                                                        }
                                                    }
                                                }

                                                vm.setMoodEvents(followedEventsList);
                                                withinFiveKmViewModel.setMoodEvents(withinFiveEventsList);
                                            })
                                            .addOnFailureListener(e -> {
                                                if (binding == null || !isValidFragment()) return;
                                                Log.e("Dashboard", "Error loading followed mood events", e);
                                            });
                                }
                            })
                            .addOnFailureListener(e -> {
                                if (binding == null || !isValidFragment()) return;
                                Log.e("Dashboard", "Error loading following users", e);
                            });
                })
                .addOnFailureListener(e -> {
                    if (binding == null || !isValidFragment()) return;
                    Log.e("CurrentLocation", "Error retrieving location", e);
                    Toast.makeText(getContext(), "Error retrieving location", Toast.LENGTH_SHORT).show();
                });
    }

    private void loadMoodEvents() {
        if (binding == null || moodEventRef == null || !isValidFragment()) return;

        SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        String currentUserId = sharedPreferences.getString("userID", null);
        if (currentUserId == null) {
            Log.e("Dashboard", "User ID is null");
            return;
        }

        moodEventRef.get()
                .addOnCompleteListener(task -> {
                    if (binding == null || !isValidFragment()) return;

                    //convert each document to
                    if (task.isSuccessful()) {
                        List<MoodEvent> moodEvents = new ArrayList<>();
                        //each documentSnapsho is a document fethced from task.getResult()
                        for (DocumentSnapshot document : task.getResult()) {
                            MoodEvent moodEvent = document.toObject(MoodEvent.class); //convert to MoodEvent class
                            if (moodEvent != null) {
                                moodEvents.add(moodEvent); //add to array
                                // Ensure user info is set from current session
                                moodEvent.setUserId(userID);
                                moodEvent.setUserName(username);
                            }
                        }

                        Log.d("MapDebug", "dashboard reached" );
                        // query first in upper space for map to draw markers
                        MoodEventsViewModel vm = new ViewModelProvider(requireActivity()).get(MoodEventsViewModel.class);
                        vm.setMoodEvents(moodEvents);
                        Log.d("MapDebug", "size dashboard" + moodEvents.size() );

                        //sort the mood events by time in descending order (most recent first)
                        Collections.sort(moodEvents, (e1, e2) -> Long.compare(e2.getTime(), e1.getTime()));

                        //limit the list to the most recent 7 mood events
                        List<MoodEvent> recentMoodEvents = new ArrayList<>();

                        //store the most recent 7 moodevent or smaller.
                        for (int i = 0; i < Math.min(7, moodEvents.size()); i++) {
                            recentMoodEvents.add(moodEvents.get(i));
                        }

                        //set the adapter with the recent 7 mood events
                        MoodEventArrayAdapter adapter = new MoodEventArrayAdapter(getContext(), recentMoodEvents);
                        binding.activityList.setAdapter(adapter);

                        //set item click listener
                        binding.activityList.setOnItemClickListener((parent, view, position, id) -> {
                            if (position >= 0 && position < recentMoodEvents.size()) {
                                //get the selected MoodEvent based on position
                                MoodEvent selectedMoodEvent = recentMoodEvents.get(position);

                                //create a bundle and put the selected MoodEvent in it
                                Bundle args = new Bundle();
                                args.putSerializable("selected_mood_event", selectedMoodEvent);
                                args.putString("source", "dashboard");

                                //navigate to inputdialog and pass the selected mood event
                                Navigation.findNavController(view).navigate(R.id.action_dashboardFragment_to_inputDialog, args);
                            }
                        });

                        //long click to delete
                        binding.activityList.setOnItemLongClickListener((parent, view, position, id) -> {
                            if (position >= 0 && position < recentMoodEvents.size()) {
                                selectedMoodForDeletion = recentMoodEvents.get(position);

                                ConfirmDeleteDialogFragment dialog = ConfirmDeleteDialogFragment.newInstance(selectedMoodForDeletion.getId());
                                dialog.show(getParentFragmentManager(), "ConfirmDeleteDialog");
                            }
                            return true; //indicate the event was handled
                        });

                    } else {
                        Log.e("Dashboard", "Error fetching mood events", task.getException());
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}