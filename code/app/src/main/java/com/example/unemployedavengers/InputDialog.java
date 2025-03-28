/*
 * InputDialog - DialogFragment for adding or editing mood events.
 *
 * Purpose:
 * - Collects user inputs including mood, reason, situation, and an image.
 * - Provides UI elements such as a spinner with custom styling, image upload, and text inputs.
 * - Sends the new or updated MoodEvent back to the parent fragment via FragmentResult.
 */
package com.example.unemployedavengers;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.os.Looper;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.unemployedavengers.databinding.InputDialogBinding;
import com.example.unemployedavengers.models.MoodEvent;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

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
    private double selectedLatitude;
    private double selectedLongitude;
    private boolean locationSet = false;
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final int AUTOCOMPLETE_REQUEST_CODE = 1;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;

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

                        try {
                            int fileSize = requireContext().getContentResolver()
                                    .openInputStream(imageUri)
                                    .available(); // Get file size in bytes

                            if (fileSize > 2 * 1024 * 1024) { // 2MB limit
                                Toast.makeText(getContext(), "File size must be under 2MB", Toast.LENGTH_SHORT).show();
                                imageUri = null; // Reset imageUri
                            } else {
                                imagePreview.setImageURI(imageUri);
                            }
                        } catch (Exception e) {
                            Toast.makeText(getContext(), "Error checking file size", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    }
                });

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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext());

        // Set up the "Use Current Location" button
        view.findViewById(R.id.use_current_location_button).setOnClickListener(v -> {
            setCurrentLocation();
        });


        //get the selected MoodEvent passed from Dashboard
        if (getArguments() != null) {
            moodEvent = (MoodEvent) getArguments().getSerializable("selected_mood_event");

            if (moodEvent != null && moodEvent.getImageUri() != null && !moodEvent.getImageUri().isEmpty()) {
                Glide.with(requireContext())
                        .load(moodEvent.getImageUri())
                        .into(imagePreview);
            }

            //get the source dashboard/history
            source = getArguments().getString("source");

            //if we are updating
            if (moodEvent != null) {
                //populate the fields with the data from the selected MoodEvent
                EditText situationEditText = view.findViewById(R.id.editSocialSituation);
                EditText reasonEditText = view.findViewById(R.id.editReason);
                Spinner spinner = view.findViewById(R.id.spinnerEmotion);

                //using getter function from model to get text
                situationEditText.setText(moodEvent.getSituation());
                reasonEditText.setText(moodEvent.getReason());

                if (Objects.equals(moodEvent.getRadioSituation(), "Alone")) {
                    ((RadioButton) view.findViewById(R.id.radioAlone)).setChecked(true);
                } else if (Objects.equals(moodEvent.getRadioSituation(), "Two or Several")) {
                    ((RadioButton) view.findViewById(R.id.radioTwoSeveral)).setChecked(true);
                } else if (Objects.equals(moodEvent.getRadioSituation(), "A Crowd")) {
                    ((RadioButton) view.findViewById(R.id.radioCrowd)).setChecked(true);
                }else if (Objects.equals(moodEvent.getRadioSituation(), "None")) {
                    ((RadioButton) view.findViewById(R.id.radioNone)).setChecked(true);
                }

                // Check if the view exists before using it
                RadioButton publicRadio = view.findViewById(R.id.radioPublicStatus);
                RadioButton privateRadio = view.findViewById(R.id.radioPrivateStatus);

                if (publicRadio != null && privateRadio != null) {
                    try {
                        if (moodEvent.getPublicStatus()){
                            publicRadio.setChecked(true);
                        } else {
                            privateRadio.setChecked(true);
                        }
                    } catch (Exception e) {
                        Log.d("InputDialog", "Public status not supported in this MoodEvent model");
                    }
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
            //get all relevant information
            String mood = (String) binding.spinnerEmotion.getSelectedItem();
            String reason = binding.editReason.getText().toString();
            String situation = binding.editSocialSituation.getText().toString();
            long time = System.currentTimeMillis();


            // Check if radioPublicStatus exists before accessing it
            boolean publicStatus = false;
            if (binding.radioPublicStatus != null) {
                publicStatus = binding.radioPublicStatus.isChecked();
            }

            String radioSituation = "Not Set";
            try {
                radioSituation = ((RadioButton) v.getRootView().findViewById(binding.radioGroupSocial.getCheckedRadioButtonId())).getText().toString();
            }
            catch (Exception e) {
                Log.d("Radio Group", "User did not pick a situation category");
            }

            reason = reason.trim();
            situation = situation.trim();

            //if moodEvent exists, update it otherwise create a new one
            if (moodEvent != null) {
                moodEvent.setMood(mood);
                moodEvent.setReason(reason);
                moodEvent.setSituation(situation);
                moodEvent.setRadioSituation(radioSituation);

                // Only set public status if the model supports it
                try {
                    moodEvent.setPublicStatus(publicStatus);
                } catch (Exception e) {
                    Log.d("InputDialog", "Public status not supported in this MoodEvent model");
                }

                // No need to change the time because we are editing the existing event
                uploadImage(moodEvent);
            } else {
                uploadNewEvent(mood, reason, situation, time, radioSituation, publicStatus);
            }

            if (Objects.equals(source, "dashboard")) {
                Navigation.findNavController(v)
                        .navigate(R.id.action_inputDialog_to_dashboardFragment);
            } else if (Objects.equals(source, "history")){
                Navigation.findNavController(v)
                        .navigate(R.id.action_inputDialog_to_historyFragment);
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
    // Method to use current location
    private void setCurrentLocation() {
        // Ensure the fragment's view is available
        View view = getView();
        if (view == null) {
            Log.e("CurrentLocation", "Fragment view is null");
            return;
        }

        // Get a reference to the ProgressBar
        ProgressBar progressBar = view.findViewById(R.id.input_address_progress_bar);
        if (progressBar == null) {
            Log.e("CurrentLocation", "ProgressBar not found in layout");
            return;
        }

        // Show the ProgressBar (animation starts automatically with indeterminate style)
        progressBar.setVisibility(View.VISIBLE);

        // Check location permissions
        if (ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(getContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(getActivity(),
                    new String[]{
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_PERMISSION_REQUEST_CODE);
            progressBar.setVisibility(View.GONE);
            return;
        }

        // Request the current location
        fusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(location -> {
                    // Hide the progress bar once a response is received
                    progressBar.setVisibility(View.GONE);
                    if (location != null) {
                        selectedLatitude = location.getLatitude();
                        selectedLongitude = location.getLongitude();
                        locationSet = true;
                        Toast.makeText(getContext(), "Current location set.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Unable to retrieve current location", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e("CurrentLocation", "Error retrieving location", e);
                    Toast.makeText(getContext(), "Error retrieving location", Toast.LENGTH_SHORT).show();
                });
    }

    // Handle the permission request result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, try setting current location again
                setCurrentLocation();
            } else {
                Toast.makeText(getContext(), "Location permission is required", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    // Helper method to send result
    private void sendResultToParent(MoodEvent event) {
        Bundle result = new Bundle();
        result.putSerializable("mood_event_key", event);
        getParentFragmentManager().setFragmentResult("input_dialog_result", result);
    }

    private void uploadNewEvent(String mood, String reason,
                                String situation, long time, String radioSituation, boolean publicStatus) {
        final MoodEvent newMoodEvent;
        try {
            newMoodEvent = new MoodEvent(mood, reason, situation, time, radioSituation, "", publicStatus);
        } catch (NoSuchMethodError methodError) {
            final MoodEvent tempMoodEvent = new MoodEvent(mood, reason, situation, time, radioSituation, "");
            try {
                tempMoodEvent.setPublicStatus(publicStatus);
            } catch (Exception ex) {
                Log.d("InputDialog", "Public status not supported in this MoodEvent model");
            }
            // Update location if it was set
            if (locationSet) {
                tempMoodEvent.setLatitude(selectedLatitude);
                tempMoodEvent.setLongitude(selectedLongitude);
                tempMoodEvent.setHasLocation(true);
            }
            if (imageUri == null) {
                sendResultToParent(tempMoodEvent);
                return;
            }
            // Image upload code remains unchanged...
            StorageReference storageRef = storage.getReference();
            StorageReference imageRef = storageRef.child("mood_images/" + UUID.randomUUID() + ".jpg");
            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                tempMoodEvent.setImageUri(uri.toString());
                                sendResultToParent(tempMoodEvent);
                            }))
                    .addOnFailureListener(uploadError -> {
                        Toast.makeText(getContext(), "Image upload failed: " + uploadError.getMessage(), Toast.LENGTH_SHORT).show();
                        sendResultToParent(tempMoodEvent);
                    });
            return;
        }
        // For the successfully created newMoodEvent, update location if set:
        if (locationSet) {
            newMoodEvent.setLatitude(selectedLatitude);
            newMoodEvent.setLongitude(selectedLongitude);
            newMoodEvent.setHasLocation(true);
        }
        if (imageUri == null) {
            sendResultToParent(newMoodEvent);
            return;
        }
        // Continue with image upload (code remains unchanged)
        StorageReference storageRef = storage.getReference();
        StorageReference imageRef = storageRef.child("mood_images/" + UUID.randomUUID() + ".jpg");
        final MoodEvent finalMoodEvent = newMoodEvent;
        imageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            finalMoodEvent.setImageUri(uri.toString());
                            sendResultToParent(finalMoodEvent);
                        }))
                .addOnFailureListener(uploadError -> {
                    Toast.makeText(getContext(), "Image upload failed: " + uploadError.getMessage(), Toast.LENGTH_SHORT).show();
                    sendResultToParent(finalMoodEvent);
                });
    }


    private void uploadImage(MoodEvent moodEvent) {
        if (imageUri != null) {
            StorageReference storageRef = storage.getReference();
            StorageReference imageRef = storageRef.child("mood_images/" + UUID.randomUUID() + ".jpg");

            // Create a final copy for the lambda
            final MoodEvent finalMoodEvent = moodEvent;

            imageRef.putFile(imageUri)
                    .addOnSuccessListener(taskSnapshot ->
                            imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                // Update the image URL and send result
                                finalMoodEvent.setImageUri(uri.toString());
                                sendResultToParent(finalMoodEvent);
                            }))
                    .addOnFailureListener(uploadError -> {
                        Toast.makeText(getContext(), "Image upload failed: " + uploadError.getMessage(), Toast.LENGTH_SHORT).show();
                        // Still send the event with its original image URL
                        sendResultToParent(finalMoodEvent);
                    });
        } else {
            // No new image to upload, send result immediately
            sendResultToParent(moodEvent);
        }
    }
}