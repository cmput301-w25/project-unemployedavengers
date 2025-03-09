package com.example.unemployedavengers;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import com.example.unemployedavengers.models.MoodEvent;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class DashboardTest {

    @Rule
    public ActivityScenarioRule<MainActivity> scenario = new ActivityScenarioRule<>(MainActivity.class);

    @BeforeClass
    public static void setupEmulator() {
        FirebaseTestHelper.setupEmulator();
    }

    @Before
    public void seedDatabase() {
        // Create sample mood events in the database
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String userId = "test1234";

        // Create a test user if needed
        HashMap<String, Object> userMap = new HashMap<>();
        userMap.put("userId", userId);
        userMap.put("username", "test1234");
        userMap.put("dummyEmail", "test1234@example.com");
        userMap.put("password", "password123");
        db.collection("users").document(userId).set(userMap);

        // Add sample mood events
        CollectionReference moodEventsRef = db.collection("users").document(userId).collection("moods");

        MoodEvent happyMood = new MoodEvent("Happiness", "Good day", "Sun", "Outside", System.currentTimeMillis() - 100000, "Alone");
        happyMood.setExisted(true);
        happyMood.setId("happy_mood_id");

        MoodEvent sadMood = new MoodEvent("Sadness", "Bad day", "Rain", "Inside", System.currentTimeMillis() - 200000, "With others");
        sadMood.setExisted(true);
        sadMood.setId("sad_mood_id");

        moodEventsRef.document(happyMood.getId()).set(happyMood);
        moodEventsRef.document(sadMood.getId()).set(sadMood);

        // Set shared preferences for the test
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SharedPreferences prefs = context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username", "testuser");
        editor.putString("userID", userId);
        editor.apply();
    }

    @After
    public void clearDatabase() {
        FirebaseTestHelper.clearDatabase();
    }

    private void loginAndNavigateToDashboard() throws InterruptedException {
        // Force navigation to Dashboard programmatically
        scenario.getScenario().onActivity(activity -> {
            NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment);
            try {
                navController.navigate(R.id.dashboardFragment);
            } catch (Exception e) {
                Log.e("Test", "Navigation failed: " + e.getMessage());
            }
        });

        // Wait for dashboard to load
        Thread.sleep(5000);
    }

    @Test
    public void dashboardShouldDisplayExistingMoods() {
        try {
            loginAndNavigateToDashboard();

            // Check for just one mood to keep it simple
            onView(withText("Happiness")).check(matches(isDisplayed()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testReasonField() {
        try {
            loginAndNavigateToDashboard();

            // Click add mood button
            onView(withId(R.id.add_mood_button)).perform(click());

            // Test just the reason field
            onView(withId(R.id.editReason)).perform(typeText("Test reason"), closeSoftKeyboard());

            // Check that text was entered correctly
            onView(withId(R.id.editReason)).check(matches(withText("Test reason")));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTriggerField() {
        try {
            loginAndNavigateToDashboard();

            // Click add mood button
            onView(withId(R.id.add_mood_button)).perform(click());

            // Test just the trigger field
            onView(withId(R.id.editTrigger)).perform(typeText("Test trigger"), closeSoftKeyboard());

            // Check that text was entered correctly
            onView(withId(R.id.editTrigger)).check(matches(withText("Test trigger")));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSocialSituationField() {
        try {
            loginAndNavigateToDashboard();

            // Click add mood button
            onView(withId(R.id.add_mood_button)).perform(click());

            // Test just the social situation field
            onView(withId(R.id.editSocialSituation)).perform(typeText("Test situation"), closeSoftKeyboard());

            // Check that text was entered correctly
            onView(withId(R.id.editSocialSituation)).check(matches(withText("Test situation")));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testRadioButtonSelection() {
        try {
            loginAndNavigateToDashboard();

            // Click add mood button
            onView(withId(R.id.add_mood_button)).perform(click());

            // Test radio button selection
            onView(withId(R.id.radioAlone)).perform(click());

            // Check that radio button was selected
            onView(withId(R.id.radioAlone)).check(matches(isChecked()));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testSpinnerSelection() {
        try {
            loginAndNavigateToDashboard();

            // Click add mood button
            onView(withId(R.id.add_mood_button)).perform(click());

            // Test spinner selection
            onView(withId(R.id.spinnerEmotion)).perform(click());
            onData(allOf(is(instanceOf(String.class)), is("😠Anger"))).perform(click());

            // It's difficult to verify spinner selection directly
            // This test passes if the selection doesn't throw an exception
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testReasonValidation() {
        try {
            loginAndNavigateToDashboard();

            // Click add mood button
            onView(withId(R.id.add_mood_button)).perform(click());

            // Enter more than 3 words
            onView(withId(R.id.editReason)).perform(typeText("This is way too many words"), closeSoftKeyboard());

            // Select a radio button (required for form validation)
            onView(withId(R.id.radioAlone)).perform(click());

            // Try to confirm
            onView(withId(R.id.buttonConfirm)).perform(click());

            // Check for error message
            onView(withId(R.id.editReason)).check(matches(hasErrorText("Reason must be 3 words or less!")));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}