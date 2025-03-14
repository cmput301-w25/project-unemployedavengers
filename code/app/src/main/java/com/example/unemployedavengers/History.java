/*
 * History Fragment - Displays the mood event history.
 *
 * Purpose:
 * - Loads mood events from Firestore.
 * - Sorts events in reverse chronological order and shows them in a ListView using MoodEventArrayAdapter.
 * - Handles user interactions for editing or deleting mood events.
 *
 */
package com.example.unemployedavengers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentResultListener;
import androidx.navigation.Navigation;

import com.example.unemployedavengers.arrayadapters.MoodEventArrayAdapter;
import com.example.unemployedavengers.databinding.HistoryBinding;
import com.example.unemployedavengers.models.MoodEvent;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;

//A history class that displays all mood event and can filter through them (to be completed later)
//All functions currently are adapted from dashboard
public class History extends Fragment {

    private HistoryBinding binding;
    private ArrayList<MoodEvent> moodList;
    private MoodEventArrayAdapter moodAdapter;
    private FirebaseFirestore db;
    private CollectionReference moodEventRef;
    private String userID;
    private MoodEvent selectedMoodForDeletion;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = HistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        userID = sharedPreferences.getString("userID", null);

        db = FirebaseFirestore.getInstance();
        moodEventRef = db.collection("users").document(userID).collection("moods");

        moodList = new ArrayList<>();
        moodAdapter = new MoodEventArrayAdapter(requireContext(), moodList);
        binding.historyList.setAdapter(moodAdapter);

        loadHistoryMoodEvents(); //load all mood events


        //register the listener for the result from InputDialog (Only once)
        getParentFragmentManager().setFragmentResultListener("input_dialog_result", getViewLifecycleOwner(), new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                //retrieve the MoodEvent object
                MoodEvent moodEvent = (MoodEvent) result.getSerializable("mood_event_key");

                //existed is true if we are updating a mood, else we are adding a mood
                if (moodEvent.getExisted()==true) {
                    updateMoodEvent(moodEvent);
                }
            }
        });

        //register the result listener for the delete confirmation
        getParentFragmentManager().setFragmentResultListener("delete_mood_event", getViewLifecycleOwner(), new FragmentResultListener() {
            @Override
            public void onFragmentResult(@NonNull String requestKey, @NonNull Bundle result) {
                boolean deleteConfirmed = result.getBoolean("DeleteConfirmed", false);
                if (deleteConfirmed) {
                    //proceed with the deletion
                    onDeleteConfirmed(selectedMoodForDeletion);
                    loadHistoryMoodEvents(); //reload the mood events after deletion
                }
            }
        });



    }

    private void updateMoodEvent(MoodEvent moodEvent) {
        //NEED TO RELOAD DATABASE CAUSE FIREBASE IS AN IDIOT (crashes if you add a moodEvent and tries to update it right away cause "cannot find id")
        loadHistoryMoodEvents();

        String moodEventId = moodEvent.getId();

        Log.d("Dashboard", "updateMoodEvent: " + moodEventId);
        //get document via id
        DocumentReference moodEventDocRef = moodEventRef.document(moodEventId);

        //set the new values for the document
        moodEventDocRef.set(moodEvent)  //use set() to update or create the document if it doesn't exist
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Mood updated successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to update mood", Toast.LENGTH_SHORT).show();
                });



    }

    public void onDeleteConfirmed(MoodEvent moodEvent) {
        moodEventRef.document(moodEvent.getId()).delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Mood deleted successfully", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to delete mood", Toast.LENGTH_SHORT).show();
                });

        loadHistoryMoodEvents();
    }

    private void loadHistoryMoodEvents() {
        moodEventRef.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        moodList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            MoodEvent moodEvent = document.toObject(MoodEvent.class);
                            moodList.add(moodEvent);
                        }
                        //sort by time (latest first)
                        Collections.sort(moodList, (e1, e2) -> Long.compare(e2.getTime(), e1.getTime()));

                        moodAdapter.notifyDataSetChanged(); //update UI

                        binding.historyList.setOnItemClickListener((parent, view, position, id) -> {
                            MoodEvent selectedEvent = moodList.get(position);
                            Bundle args = new Bundle();
                            args.putSerializable("selected_mood_event", selectedEvent);
                            args.putString("source", "history");
                            Navigation.findNavController(view)
                                    .navigate(R.id.action_historyFragment_to_inputDialog, args);
                        });

                        binding.historyList.setOnItemLongClickListener((parent, view, position, id) -> {
                            selectedMoodForDeletion = moodList.get(position);

                            ConfirmDeleteDialogFragment dialog = ConfirmDeleteDialogFragment.newInstance(selectedMoodForDeletion.getId());
                            dialog.show(getParentFragmentManager(), "ConfirmDeleteDialog");

                            return true; // Indicate the event was handled
                        });

                    } else {
                        Log.e("HistoryFragment", "Error fetching mood events", task.getException());
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
