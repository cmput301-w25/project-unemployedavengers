/*
 * UserDAOImplement - Implementation of IUserDAO interface for managing user-related operations.
 *
 * This class provides concrete implementations for user authentication, profile management,
 * and social interactions (following/unfollowing users) using Firebase Authentication
 * and Firestore as the backend database.
 *
 * Features:
 * - User registration and authentication
 * - Password and username management
 * - Follow/unfollow functionality
 * - Searching for users
 * - Retrieving user profiles
 * - Handling follow requests
 *
 * This class interacts with Firestore to store user details and Firebase Authentication for
 * secure user login and authentication management.
 *
 * @author
 * @version 1.0
 */

package com.example.unemployedavengers.implementationDAO;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.unemployedavengers.DAO.IUserDAO;
import com.example.unemployedavengers.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserDAOImplement implements IUserDAO {

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;

    public UserDAOImplement() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Registers a new user in Firebase Authentication and stores user details in Firestore.
     * <p>
     * Since Firebase Authentication requires an email, a dummy email is generated using the username.
     * After user creation, the details are stored in the Firestore database.
     * </p>
     *
     * @param username The desired username for the user.
     * @param password The password for the user.
     * @return A {@link Task<Void>} indicating the success or failure of the registration process.
     *         - On success, the user's details are stored in Firestore.
     *         - On failure, an exception is thrown.
     * @throws Exception If the user creation fails or Firebase does not return a valid user.
     */
    @Override
    public Task<Void> signUpUser(@NonNull final String username, @NonNull final String password) {
        final String dummyEmail = username.toLowerCase() + "@example.com";

        // Create user with dummyEmail and password
        return auth.createUserWithEmailAndPassword(dummyEmail, password)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        // If this fails, maybe the username is taken or the password is invalid
                        throw task.getException();
                    }

                    // Retrieve the current user
                    FirebaseUser firebaseUser = auth.getCurrentUser();
                    if (firebaseUser == null) {
                        throw new Exception("User creation failed: no FirebaseUser returned.");
                    }

                    final String userId = firebaseUser.getUid();
                    // Create a User object
                    User user = new User(userId, username, dummyEmail, password);

                    // Store user in Firestore
                    DocumentReference userDoc = db.collection("users").document(userId);
                    return userDoc.set(user);
                });
    }


    /**
     * Signs in a user using Firebase Authentication.
     * <p>
     * Since Firebase Authentication requires an email, a dummy email is reconstructed using the username.
     * The method attempts to sign in the user with the dummy email and password.
     * </p>
     *
     * @param username The username of the user attempting to sign in.
     * @param password The password associated with the username.
     * @return A {@link Task<Void>} indicating the success or failure of the sign-in process.
     *         - On success, the user is signed in.
     *         - On failure, an exception is thrown.
     * @throws Exception If authentication fails due to incorrect credentials or other issues.
     */
    @Override
    public Task<Void> signInUser(@NonNull final String username, @NonNull final String password) {
        // Reconstruct the dummy email
        final String dummyEmail = username.toLowerCase() + "@example.com";

        // Sign in with email and password
        return auth.signInWithEmailAndPassword(dummyEmail, password)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    return Tasks.forResult(null);
                });
    }
    /**
     * Checks if a user exists in Firestore.
     * <p>
     * This method queries the Firestore database for a document in the "users" collection
     * where the "username" field matches the provided username.
     * </p>
     *
     * @param username The username to check for existence.
     * @return A {@link Task<Boolean>} that:
     *         - Returns {@code true} if the user exists.
     *         - Returns {@code false} if the user does not exist.
     *         - Throws an exception if the query fails.
     * @throws Exception If there is an error fetching the user record from Firestore.
     */
    @Override
    public Task<Boolean> checkUserExists(@NonNull String username) {
        // Query the database to check if the user exists
        return db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null) {
                        throw task.getException() != null
                                ? task.getException()
                                : new Exception("Error fetching user record.");
                    }
                    // If the query returns at least one document, the user exists.
                    return !task.getResult().isEmpty();
                });
    }
    /**
     * Changes the password of a user in Firebase Authentication and updates it in Firestore.
     * <p>
     * This method first verifies that the currently authenticated user matches the user attempting
     * to change the password. If the authentication check passes, it updates the password in Firebase Authentication
     * and then updates the stored password in Firestore.
     * </p>
     *
     * @param user        The {@link User} object containing user details.
     * @param newPassword The new password to be set for the user.
     * @return A {@link Task<Void>} indicating the success or failure of the password change.
     *         - On success, the password is updated in both Firebase Authentication and Firestore.
     *         - On failure, an exception is thrown.
     * @throws Exception If the user is not logged in or if the authenticated user does not match the provided user.
     */
    public Task<Void> changePassword(@NonNull User user, @NonNull String newPassword) {
        final String dummyEmail = user.getDummyEmail();
        FirebaseUser currentUser = auth.getCurrentUser();

        // Ensure that a user is currently authenticated
        if (currentUser == null) {
            return Tasks.forException(new Exception("User not logged in"));
        }

        // Verify that the authenticated user matches the provided user
        if (!dummyEmail.equals(currentUser.getEmail())) {
            return Tasks.forException(new Exception("Authenticated user does not match the username provided"));
        }

        // Update the password in Firebase Authentication
        return currentUser.updatePassword(newPassword)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Update the stored password in Firestore
                    String uid = currentUser.getUid();
                    DocumentReference userDoc = db.collection("users").document(uid);
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("password", newPassword);
                    return userDoc.update(updates);
                });
    }

    /**
     * Resets the password of a user in Firebase Authentication and updates it in Firestore.
     * <p>
     * This method follows these steps:
     * - Queries Firestore to check if the username exists.
     * - Retrieves the stored password from Firestore.
     * - Signs in the user using Firebase Authentication with the retrieved password.
     * - Retrieves the user's profile from Firestore.
     * - Updates the user's password in both Firebase Authentication and Firestore.
     * </p>
     *
     * @param username    The username of the user whose password needs to be reset.
     * @param newPassword The new password to be set for the user.
     * @return A {@link Task<Void>} indicating the success or failure of the password reset process.
     *         - On success, the password is updated in both Firebase Authentication and Firestore.
     *         - On failure, an exception is thrown.
     * @throws Exception If the user does not exist, if authentication fails, or if the user profile cannot be retrieved.
     */
    @Override
    public Task<Void> resetPassword(@NonNull String username, @NonNull String newPassword) {
        final String dummyEmail = username.toLowerCase() + "@example.com";

        return db.collection("users")
                .whereEqualTo("username", username)
                .limit(1)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || task.getResult().isEmpty()) {
                        throw new Exception("User record not found in Firestore.");
                    }

                    // Retrieve the stored password from Firestore
                    String storedPassword = task.getResult().getDocuments().get(0).getString("password");
                    if (storedPassword == null) {
                        throw new Exception("Stored password not found.");
                    }

                    // Sign in the user first before changing the password
                    return auth.signInWithEmailAndPassword(dummyEmail, storedPassword);
                })
                .continueWithTask(getUserTask -> getCurrentUserProfile()) // Retrieve current user profile
                .continueWithTask(profileTask -> {
                    User currentUser = profileTask.getResult();
                    if (currentUser == null) {
                        throw new Exception("Failed to retrieve current user profile.");
                    }

                    // Change the password for the user
                    return changePassword(currentUser, newPassword);
                });
    }
    /**
     * Retrieves the current signed-in user's profile from Firestore.
     * <p>
     * This method:
     * - Checks if a user is currently signed in.
     * - Fetches the user's profile data from Firestore using their unique user ID.
     * - Converts the Firestore document into a {@link User} object.
     * </p>
     *
     * @return A {@link Task<User>} that:
     *         - Returns the {@link User} object if the user is signed in and the profile is found.
     *         - Throws an exception if no user is signed in or if the profile retrieval fails.
     * @throws Exception If no user is signed in or if the Firestore query fails.
     */
    @Override
    public Task<User> getCurrentUserProfile() {
        // Get the currently signed-in user from Firebase Authentication
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) {
            return Tasks.forException(new Exception("No user signed in"));
        }

        // Retrieve the user ID
        String userId = firebaseUser.getUid();
        DocumentReference userDoc = db.collection("users").document(userId);

        // Fetch the user profile from Firestore and convert it to a User object
        return userDoc.get().continueWith(task -> {
            if (!task.isSuccessful() || task.getResult() == null) {
                throw new Exception("Failed to fetch user profile");
            }
            return task.getResult().toObject(User.class);
        });
    }

    /**
     * Changes the username of a user and updates their dummy email in Firebase Authentication and Firestore.
     * <p>
     * This method:
     * - Generates a new dummy email based on the new username.
     * - Updates the user's email in Firebase Authentication.
     * - Updates the username and dummy email in Firestore.
     * </p>
     *
     * @param newUsername The new username to be assigned to the user.
     * @return A {@link Task<Void>} indicating the success or failure of the username change.
     *         - On success, the username and dummy email are updated in both Firebase Authentication and Firestore.
     *         - On failure, an exception is thrown.
     * @throws Exception If no user is signed in or if the update operation fails.
     */
    @Override
    public Task<Void> changeUsername(@NonNull String newUsername) {
        final String newDummyEmail = newUsername.toLowerCase() + "@example.com";
        FirebaseUser currentUser = auth.getCurrentUser();

        // Ensure that a user is currently signed in
        if (currentUser == null) {
            return Tasks.forException(new Exception("No user is signed in."));
        }

        // Update the user's email in Firebase Authentication
        return currentUser.updateEmail(newDummyEmail)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    // Update the username and dummy email in Firestore
                    String uid = currentUser.getUid();
                    DocumentReference userDoc = db.collection("users").document(uid);
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("username", newUsername);
                    updates.put("dummyEmail", newDummyEmail);
                    return userDoc.update(updates);
                });
    }

    /**
     * Sends a follow request from one user to another.
     * <p>
     * This method:
     * - Creates a follow request document in the target user's "requests" subcollection.
     * - Stores request details such as requester ID, request status, and timestamp.
     * </p>
     *
     * @param requesterId The user ID of the user who is sending the follow request.
     * @param targetId The user ID of the target user who will receive the follow request.
     * @return A {@link Task<Void>} indicating the success or failure of the request operation.
     *         - On success, a follow request is created in Firestore.
     *         - On failure, an exception is thrown.
     */
    @Override
    public Task<Void> requestFollow(@NonNull String requesterId, @NonNull String targetId) {
        // Reference to the follow request document in the target user's "requests" subcollection
        DocumentReference requestDocRef = db.collection("users")
                .document(targetId)
                .collection("requests")
                .document(requesterId);

        // Prepare the request data
        Map<String, Object> requestData = new HashMap<>();
        requestData.put("status", "pending"); // Mark the request as pending
        requestData.put("requestedAt", System.currentTimeMillis()); // Store timestamp
        requestData.put("requesterId", requesterId); // Store requester ID

        // Store the follow request in Firestore
        return requestDocRef.set(requestData);
    }


    /**
     * Accepts a follow request and updates the follower/following relationship in Firestore.
     * <p>
     * This method:
     * - Deletes the follow request document from the target user's "requests" subcollection.
     * - Adds an entry in the requester's "following" subcollection to mark the target as followed.
     * - Adds an entry in the target user's "followers" subcollection to mark the requester as a follower.
     * - Uses a Firestore batch operation to ensure atomicity.
     * </p>
     *
     * @param requesterId The user ID of the user who sent the follow request.
     * @param targetId The user ID of the user accepting the follow request.
     * @return A {@link Task<Void>} indicating the success or failure of the operation.
     *         - On success, the follow request is removed, and the follower relationship is created.
     *         - On failure, an exception is thrown.
     */
    @Override
    public Task<Void> acceptFollowRequest(@NonNull String requesterId, @NonNull String targetId) {
        // Reference to the follow request document to be deleted
        DocumentReference requestDocRef = db.collection("users")
                .document(targetId)
                .collection("requests")
                .document(requesterId);

        // Reference to the requester's "following" subcollection
        DocumentReference followerFollowingRef = db.collection("users")
                .document(requesterId)
                .collection("following")
                .document(targetId);

        // Reference to the target user's "followers" subcollection
        DocumentReference followedFollowersRef = db.collection("users")
                .document(targetId)
                .collection("followers")
                .document(requesterId);

        long timestamp = System.currentTimeMillis();

        // Data to be stored in the requester's "following" subcollection
        Map<String, Object> followingData = new HashMap<>();
        followingData.put("followedId", targetId);
        followingData.put("followedAt", timestamp);

        // Data to be stored in the target user's "followers" subcollection
        Map<String, Object> followerData = new HashMap<>();
        followerData.put("followerId", requesterId);
        followerData.put("followedAt", timestamp);

        // Use a Firestore batch to execute all operations atomically
        WriteBatch batch = db.batch();
        batch.delete(requestDocRef); // Remove the follow request
        batch.set(followerFollowingRef, followingData); // Add target to requester's following list
        batch.set(followedFollowersRef, followerData); // Add requester to target's followers list

        return batch.commit()
                .addOnSuccessListener(aVoid -> {
                    Log.d("FollowRequest", "Follow request accepted successfully");
                })
                .addOnFailureListener(e -> {
                    Log.e("FollowRequest", "Failed to accept follow request", e);
                });
    }

    /**
     * Rejects a follow request by deleting the request document from Firestore.
     * <p>
     * This method:
     * - Locates the follow request document in the target user's "requests" subcollection.
     * - Deletes the request document, effectively rejecting the follow request.
     * </p>
     *
     * @param requesterId The user ID of the user who sent the follow request.
     * @param targetId The user ID of the user rejecting the follow request.
     * @return A {@link Task<Void>} indicating the success or failure of the operation.
     *         - On success, the follow request is deleted.
     *         - On failure, an exception is thrown.
     */
    @Override
    public Task<Void> rejectFollowRequest(@NonNull String requesterId, @NonNull String targetId) {
        // Reference to the follow request document in Firestore
        DocumentReference requestDocRef = db.collection("users")
                .document(targetId)
                .collection("requests")
                .document(requesterId);

        // Delete the follow request document
        return requestDocRef.delete();
    }


    /**
     * Unfollows a user by removing the follow relationship from Firestore.
     * <p>
     * This method:
     * - Removes the target user from the requester's "following" subcollection.
     * - Removes the requester from the target user's "followers" subcollection.
     * - Uses a Firestore batch operation to ensure atomic execution of deletions.
     * </p>
     *
     * @param followerId The user ID of the user who wants to unfollow.
     * @param followedId The user ID of the user being unfollowed.
     * @return A {@link Task<Void>} indicating the success or failure of the unfollow operation.
     *         - On success, the follow relationship is removed.
     *         - On failure, an exception is thrown.
     */
    @Override
    public Task<Void> unfollowUser(@NonNull String followerId, @NonNull String followedId) {
        // Reference to the follow relationship in the follower's "following" subcollection
        DocumentReference followerFollowingRef = db.collection("users")
                .document(followerId)
                .collection("following")
                .document(followedId);

        // Reference to the follow relationship in the followed user's "followers" subcollection
        DocumentReference followedFollowersRef = db.collection("users")
                .document(followedId)
                .collection("followers")
                .document(followerId);

        // Use a Firestore batch to execute both deletions atomically
        WriteBatch batch = db.batch();
        batch.delete(followerFollowingRef); // Remove from follower's "following"
        batch.delete(followedFollowersRef); // Remove from followed user's "followers"

        return batch.commit(); // Execute the batch operation
    }



    /**
     * Searches for users in Firestore whose usernames match or start with the given search term.
     * <p>
     * This method:
     * - Queries the Firestore "users" collection for usernames matching the search term.
     * - Retrieves the current user's profile to exclude them from the search results.
     * - Uses Firestore's `orderBy` and range filtering (`startAt` and `endAt`) to efficiently find matching users.
     * - Returns a list of users who match the search query, excluding the currently signed-in user.
     * </p>
     *
     * @param userName The search term to find matching usernames.
     * @return A {@link Task<List<User>>} containing the list of matching users.
     *         - On success, returns a list of users whose usernames start with the given search term.
     *         - On failure, an exception is thrown.
     */
    @Override
    public Task<List<User>> searchUsers(@NonNull String userName) {
        // Query Firestore for users whose usernames match the search term
        Task<QuerySnapshot> queryTask = db.collection("users")
                .orderBy("username")
                .startAt(userName)
                .endAt(userName + "\uf8ff") // Firestore range filtering to get usernames that start with the given term
                .get();

        // Retrieve the current user's profile to exclude them from the results
        Task<User> currentUserTask = getCurrentUserProfile();

        // Execute both tasks and process results when all tasks complete successfully
        return Tasks.whenAllSuccess(queryTask, currentUserTask)
                .continueWith(task -> {
                    // Extract the results from both tasks
                    List<Object> results = task.getResult();
                    QuerySnapshot querySnapshot = (QuerySnapshot) results.get(0); // Retrieved user list
                    User currentUser = (User) results.get(1); // Current user profile
                    String currentUid = currentUser.getUserId();

                    // Filter out the current user and add matching users to the result list
                    List<User> userList = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        User user = doc.toObject(User.class);
                        if (user != null && !user.getUserId().equals(currentUid)) {
                            userList.add(user);
                        }
                    }
                    return userList;
                });
    }

    /**
     * Retrieves a user from Firestore based on their username.
     * <p>
     * This method:
     * - Queries the Firestore "users" collection for a document where the "username" field matches the given username.
     * - Limits the query to return only one document.
     * - Converts the retrieved Firestore document into a {@link User} object.
     * - Returns {@code null} if no matching user is found.
     * </p>
     *
     * @param username The username of the user to be retrieved.
     * @return A {@link Task<User>} containing the user data if found, or {@code null} if no user exists with the given username.
     *         - On success, returns a {@link User} object.
     *         - On failure, an exception may be thrown.
     */
    @Override
    public Task<User> getUserByUsername(@NonNull String username) {
        return db.collection("users")
                .whereEqualTo("username", username) // Query Firestore for the given username
                .limit(1) // Limit the query to return only one user
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || task.getResult().isEmpty()) {
                        return null; // Return null if the user is not found
                    }
                    return task.getResult().getDocuments().get(0).toObject(User.class); // Convert Firestore document to User object
                });
    }


}
