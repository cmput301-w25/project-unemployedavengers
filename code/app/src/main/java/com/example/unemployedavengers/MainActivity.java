/*
 * MainActivity - Entry point and navigation handler for the Unemployed Avengers Android application.
 *
 * This class sets up:
 * - View binding for UI management.
 * - Navigation component to manage fragment transitions.
 * - Bottom navigation visibility and interaction based on user authentication state.
 *
 * Features:
 * - Uses  Navigation to manage fragment navigation.
 * - Controls the visibility of the BottomNavigationView based on active fragments.
 *
 */

package com.example.unemployedavengers;

import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.example.unemployedavengers.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private NavController navController;

    /**
     * Called when the activity is first created.
     *
     * @param savedInstanceState The saved instance state bundle.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Set up the Navigation Component with the NavHostFragment
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);

        // Handle bottom navigation item selection
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            // Prevent unnecessary navigation to the current destination
            if (navController.getCurrentDestination().getId() != item.getItemId()) {
                navController.navigate(item.getItemId());
            }
            return true;
        });

        // Manage the visibility of the BottomNavigationView based on the active fragment
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.loginFragment ||
                    destination.getId() == R.id.signUpFragment ||
                    destination.getId() == R.id.passwordReset1Fragment ||
                    destination.getId() == R.id.passwordReset2Fragment ||
                    destination.getId() == R.id.homeFragment) {
                // Hide bottom navigation when in authentication-related fragments
                binding.bottomNavigation.setVisibility(View.GONE);
            } else {
                // Show bottom navigation for other fragments
                binding.bottomNavigation.setVisibility(View.VISIBLE);
            }
        });
    }
}
