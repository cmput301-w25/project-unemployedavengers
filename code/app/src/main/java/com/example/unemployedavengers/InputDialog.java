package com.example.unemployedavengers;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.unemployedavengers.databinding.InputDialogBinding;
import com.example.unemployedavengers.models.MoodEvent;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link InputDialog#newInstance} factory method to
 * create an instance of this fragment.
 */

/*
This class interacts with the input dialog xml and retrieve inputted information.
It also has the adapter for the spinner as well as colouring the text for the mood
After user input info they can either confirm which will send a new moodEvent back to dashboard or
cancel and go right back to dashboard
 */
public class InputDialog extends DialogFragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;
    private InputDialogBinding binding; //binding

    private MoodEvent moodEvent;
    private String source;

    private Uri imageUri;
    private FirebaseStorage storage;
    private String imageUrl = "";
    private ImageView imagePreview;
    private Button btnUploadImage;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<String> permissionLauncher;

    /**
     * A empty constructor needed
     */
    public InputDialog() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment InputDialog.
     */
    // TODO: Rename and change types and number of parameters
    public static InputDialog newInstance(String param1, String param2) {
        InputDialog fragment = new InputDialog();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }


    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        //create the binding
        binding = InputDialogBinding.inflate(inflater, container, false);
        storage = FirebaseStorage.getInstance();

        //get the spinner
        Spinner spinnerEmotion = binding.spinnerEmotion;

        imagePreview = binding.imagePreview;
        btnUploadImage = binding.buttonUploadPicture;

        setupImagePickerLaunchers();

        //create the adapter
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                getContext(),
                R.array.spinner_items,
                android.R.layout.simple_spinner_item
        );

        // Customize the dropdown view and spinner item appearance
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item); // Style for the dropdown view

        // Override getView to customize the selected item view
        adapter = new ArrayAdapter<CharSequence>(getContext(), android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.spinner_items)) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;

                // Change the text color based on the item position
                switch (position) {
                    case 0: // Anger
                        textView.setTextColor(Color.RED);
                        break;
                    case 1: // Confusion

                        textView.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.orange));

                        break;
                    case 2: // Disgust
                        textView.setTextColor(Color.GREEN);
                        break;
                    case 3: // Fear
                        textView.setTextColor(Color.BLUE);
                        break;
                    case 4: // Happiness
                        textView.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.baby_blue));
                        break;
                    case 5: // Sadness
                        textView.setTextColor(Color.GRAY);
                        break;
                    case 6: // Shame
                        textView.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.yellow));
                        break;
                    case 7: // Surprise
                        textView.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.pink));
                        break;
                    default:
                        textView.setTextColor(Color.BLACK);
                        break;
                }
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView textView = (TextView) view;

                // You can change the color for the dropdown items as well
                switch (position) {
                    case 0:
                        textView.setTextColor(Color.RED);
                        break;
                    case 1:
                        textView.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.orange));
                        break;
                    case 2:
                        textView.setTextColor(Color.GREEN);
                        break;
                    case 3:
                        textView.setTextColor(Color.BLUE);
                        break;
                    case 4:
                        textView.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.baby_blue));
                        break;
                    case 5:
                        textView.setTextColor(Color.GRAY);
                        break;
                    case 6:
                        textView.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.yellow));
                        break;
                    case 7:
                        textView.setTextColor(ContextCompat.getColor(parent.getContext(), R.color.pink));
                        break;
                    default:
                        textView.setTextColor(Color.BLACK);
                        break;
                }
                return view;
            }
        };

        //set the adapter to the spinner
        spinnerEmotion.setAdapter(adapter);

        return binding.getRoot();

    }

    private void setupImagePickerLaunchers() {
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        imageUri = result.getData().getData();
                        imagePreview.setImageURI(imageUri);
                    }
                });

        // Permission launcher
        permissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        imagePickerLauncher.launch(intent);
                    } else {
                        Toast.makeText(getContext(), "Permission required", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        btnUploadImage.setOnClickListener(v -> {
            imagePickerLauncher.launch(new Intent(MediaStore.ACTION_PICK_IMAGES));
        });

        //get the selected MoodEvent passed from Dashboard
        if (getArguments() != null) {
            moodEvent = (MoodEvent) getArguments().getSerializable("selected_mood_event");

            if (moodEvent != null && moodEvent.getImageUri() != null && !moodEvent.getImageUri().isEmpty()) {
                Glide.with(requireContext())
                        .load(moodEvent.getImageUri())
                        .into(imagePreview);
            }

            source = getArguments().getString("source");
            if (moodEvent != null) {
                //populate the fields with the data from the selected MoodEvent
                EditText triggerEditText = view.findViewById(R.id.editTrigger);
                EditText situationEditText = view.findViewById(R.id.editSocialSituation);
                EditText reasonEditText = view.findViewById(R.id.editReason);
                Spinner spinner =  view.findViewById(R.id.spinnerEmotion);

                //using getter function from model to get text
                triggerEditText.setText(moodEvent.getTrigger());
                situationEditText.setText(moodEvent.getSituation());
                reasonEditText.setText(moodEvent.getReason());

                if (Objects.equals(moodEvent.getRadioSituation(), "Alone")) {
                    ((RadioButton) view.findViewById(R.id.radioAlone)).setChecked(true);
                } else if (Objects.equals(moodEvent.getRadioSituation(), "Two or Several")) {
                    ((RadioButton) view.findViewById(R.id.radioTwoSeveral)).setChecked(true);
                } else if (Objects.equals(moodEvent.getRadioSituation(), "A Crowd")) {
                    ((RadioButton) view.findViewById(R.id.radioCrowd)).setChecked(true);
                }


                //using the adapter in onCreateview
                ArrayAdapter<CharSequence> adapter = (ArrayAdapter<CharSequence>) spinner.getAdapter();

                String selectedMood = moodEvent.getMood();

                //match the position to the adapter position and set it to that mood
                int position = adapter.getPosition(selectedMood);
                if (position != -1) {
                    spinner.setSelection(position);
                }

            }
        }


        //when user clicks confirm
        binding.buttonConfirm.setOnClickListener(v -> {
            if (binding.editReason.getText().toString().split(" ").length <= 3) {
                //get all relevant information
                String mood = (String) binding.spinnerEmotion.getSelectedItem();
                String reason = binding.editReason.getText().toString();
                String trigger = binding.editTrigger.getText().toString();
                String situation = binding.editSocialSituation.getText().toString();
                long time = System.currentTimeMillis();

                String radioSituation = "Not Set";
                try {
                    radioSituation = ((RadioButton) v.getRootView().findViewById(binding.radioGroupSocial.getCheckedRadioButtonId())).getText().toString();
                }
                catch (Exception e) {
                    Log.d("Radio Group", "User did not pick a situation category");
                }

                reason = reason.trim();
                trigger = trigger.trim();
                situation = situation.trim();

                //if moodEvent exists, update it otherwise create a new one
                if (moodEvent != null) {
                    moodEvent.setMood(mood);
                    moodEvent.setReason(reason);
                    moodEvent.setTrigger(trigger);
                    moodEvent.setSituation(situation);
                    moodEvent.setRadioSituation(radioSituation);
                    //no need to change the time because we are editing the existing event
                    uploadImage(moodEvent);
                } else uploadNewEvent(moodEvent, mood, reason, trigger, situation, time, radioSituation);

                if (Objects.equals(source, "dashboard")) {
                    Navigation.findNavController(v)
                            .navigate(R.id.action_inputDialog_to_dashboardFragment);
                } else if (Objects.equals(source, "history")){
                    Navigation.findNavController(v)
                            .navigate(R.id.action_inputDialog_to_historyFragment);
                }
            } else {
                binding.editReason.setError("Reason must be 3 words or less!");
            }
        });


        //go right back to dashboard
        binding.buttonCancel.setOnClickListener(v ->{
            Toast.makeText(getContext(), "Action Cancelled", Toast.LENGTH_SHORT).show();
            if (Objects.equals(source, "dashboard")) {
                Navigation.findNavController(v)
                        .navigate(R.id.action_inputDialog_to_dashboardFragment);
            } else if (Objects.equals(source, "history")){
                Navigation.findNavController(v)
                        .navigate(R.id.action_inputDialog_to_historyFragment);
            }
        });
    }

    private void uploadNewEvent(MoodEvent moodEvent, String mood, String reason, String trigger,
                                String situation, long time, String radioSituation) {
        // Create a new MoodEvent with empty image URL initially
        MoodEvent newMoodEvent = new MoodEvent(mood, reason, trigger, situation, time, radioSituation, "");

        if (imageUri != null) {
            StorageReference storageRef = storage.getReference();
            StorageReference imageRef = storageRef.child("mood_images/" + UUID.randomUUID() + ".jpg");

            // Show loading indicator if needed

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                // Update the image URL and send result
                                newMoodEvent.setImageUri(uri.toString());
                                sendResultToParent(newMoodEvent);
                            }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Still send the event, just without an image
                        sendResultToParent(newMoodEvent);
                    });
        } else {
            // No image to upload, send result immediately
            sendResultToParent(newMoodEvent);
        }
    }

    // Helper method to send result
    private void sendResultToParent(MoodEvent event) {
        Bundle result = new Bundle();
        result.putSerializable("mood_event_key", event);
        getParentFragmentManager().setFragmentResult("input_dialog_result", result);
    }
    private void uploadImage(MoodEvent moodEvent) {
        if (imageUri != null) {
            StorageReference storageRef = storage.getReference();
            StorageReference imageRef = storageRef.child("mood_images/" + UUID.randomUUID() + ".jpg");

            // Show loading indicator if needed

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                // Update the image URL and send result
                                moodEvent.setImageUri(uri.toString());
                                sendResultToParent(moodEvent);
                            }))
                    .addOnFailureListener(e -> {
                        Toast.makeText(getContext(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        // Still send the event with its original image URL
                        sendResultToParent(moodEvent);
                    });
        } else {
            // No new image to upload, send result immediately
            sendResultToParent(moodEvent);
        }
    }
}