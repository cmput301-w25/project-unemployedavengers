/**
 * BaseFragment - A base class for Fragments providing utility methods to handle common fragment tasks.
 *
 * Purpose:
 * - Provides navigation methods to prevent rapid, multiple clicks (debouncing).
 * - Offers safe navigation even with multiple click attempts or invalid fragment states.
 * - Ensures that navigation is only performed if the fragment is in a valid state and can be interacted with.
 * - Helps manage user interactions to avoid issues caused by fast, repeated clicks (common in navigation scenarios).
 *
 * Key Features:
 * - Debounced click handling: Prevents multiple navigations in quick succession using the `isClickTooSoon()` method.
 * - Safe navigation: Includes methods like `safeNavigate()` to ensure navigation happens only when appropriate.
 * - Fragment state checks: Ensures fragment operations are performed only when the fragment is valid (attached, not removing, not detached).
 *
 * Outstanding Issues:
 * - Currently, the method `safeNavigate()` does not handle cases where the navigation action itself may fail (e.g., if the destination is unavailable).
 * - Consider adding visual feedback to the user when clicks are debounced, to improve UX.
 */
package com.example.unemployedavengers;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

public class BaseFragment extends Fragment {
    protected long lastClickTime = 0;
    private static final long CLICK_DELAY = 1000; // 1 second debounce

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Helper method to prevent rapid clicking
     * @return true if this click should be ignored, false if it's okay to process
     */
    protected boolean isClickTooSoon() {
        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime - lastClickTime < CLICK_DELAY) {
            return true; // Ignore this click
        }
        lastClickTime = currentTime;
        return false; // Process this click
    }

    /**
     * Navigate safely even if multiple clicks happen
     * @param view The view containing the NavController
     * @param actionId The navigation action ID
     */
    protected void safeNavigate(View view, int actionId) {
        if (isClickTooSoon() || !isAdded()) {
            return;
        }

        try {
            NavController navController = Navigation.findNavController(view);
            // Check if we're already at the destination
            if (navController.getCurrentDestination().getId() != actionId) {
                navController.navigate(actionId);
            }
        } catch (Exception e) {
            Log.e("BaseFragment", "Navigation error: " + e.getMessage(), e);
        }
    }

    /**
     * Navigate safely with arguments
     * @param view The view containing the NavController
     * @param actionId The navigation action ID
     * @param args Bundle of arguments
     */
    protected void safeNavigate(View view, int actionId, Bundle args) {
        if (isClickTooSoon() || !isAdded()) {
            return;
        }

        try {
            NavController navController = Navigation.findNavController(view);
            navController.navigate(actionId, args);
        } catch (Exception e) {
            Log.e("BaseFragment", "Navigation error with args: " + e.getMessage(), e);
        }
    }

    /**
     * Check if the fragment is still valid for UI operations
     * @return true if fragment is attached and can be used
     */
    protected boolean isValidFragment() {
        return isAdded() && getContext() != null && !isRemoving() && !isDetached();
    }
}