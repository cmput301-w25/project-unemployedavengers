/*
 * SignUp Fragment for the Unemployed Avengers Android application.
 *
 * This fragment handles user registration, including:
 * - Inflating the sign-up layout using view binding.
 * - Validating user input for username, password, and confirmation.
 * - Registering the user via the UserDAO.
 * - Navigating to the Login screen upon successful sign-up.
 * - Displaying error messages with a custom Toast on failure.
 */

package com.example.unemployedavengers.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.unemployedavengers.DAO.IUserDAO;
import com.example.unemployedavengers.R;
import com.example.unemployedavengers.databinding.SignUpBinding;
import com.example.unemployedavengers.implementationDAO.UserDAOImplement;
import com.google.android.gms.tasks.Task;

public class SignUp extends Fragment {

    private SignUpBinding binding;
    private IUserDAO userDAO;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the view using view binding
        binding = SignUpBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view,
                              @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize the User Data Access Object
        userDAO = new UserDAOImplement();

        // Navigate back to Home fragment when Back button is clicked
        binding.btnBack.setOnClickListener(v -> {
            Navigation.findNavController(v)
                    .navigate(R.id.action_signUpFragment_to_homeFragment);
        });

        // Handle Sign-Up button click for user registration
        binding.btnSignup.setOnClickListener(v -> {
            String username = binding.etUsername.getText().toString().trim();
            String password = binding.etPassword.getText().toString().trim();
            String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

            // Validate that all fields are filled
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
                Toast.makeText(getContext(), "All fields are required.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Validate that password and confirm password match
            if (!password.equals(confirmPassword)) {
                Toast.makeText(getContext(), "Passwords do not match!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Attempt to sign up the user using the DAO
            Task<Void> signUpTask = userDAO.signUpUser(username, password);
            signUpTask
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(getContext(), "Sign up successful!", Toast.LENGTH_SHORT).show();
                        // Navigate to login screen after successful sign-up
                        Navigation.findNavController(v)
                                .navigate(R.id.action_signUpFragment_to_loginFragment);
                    })
                    .addOnFailureListener(e -> {
                        // Inflate custom toast layout to display error message
                        LayoutInflater inflater = getLayoutInflater();
                        View layout = inflater.inflate(R.layout.custom_toast, null);
                        TextView toastText = layout.findViewById(R.id.toastText);

                        // Workaround for specific error messages
                        String errorMessage = e.getMessage();
                        if (errorMessage != null && errorMessage.contains("email address is")) {
                            errorMessage = "Username already taken. Please choose a different one.";
                        }
                        toastText.setText("Sign up failed: " + errorMessage);

                        Toast toast = new Toast(getContext());
                        toast.setDuration(Toast.LENGTH_LONG);
                        toast.setView(layout);
                        toast.show();
                    });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Prevent memory leaks by nullifying the binding
        binding = null;
    }
}