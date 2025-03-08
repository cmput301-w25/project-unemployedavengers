package com.example.unemployedavengers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.unemployedavengers.arrayadapters.MoodEventArrayAdapter;
import com.example.unemployedavengers.databinding.HistoryBinding;
import com.example.unemployedavengers.models.MoodEvent;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Collections;
//largely adapted from dashboard
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

                        //removed limit
                        moodAdapter.notifyDataSetChanged(); //update UI
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
