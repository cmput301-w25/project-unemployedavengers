package com.example.unemployedavengers;

import static org.junit.Assert.*;

import com.example.unemployedavengers.implementationDAO.UserDAOImplement;
import com.example.unemployedavengers.models.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class UserDAOImplementTest {

    private UserDAOImplement userDAO;
    private FirebaseAuth auth;

    @Before
    public void setUp() {
        userDAO = new UserDAOImplement();
        auth = FirebaseAuth.getInstance();
        auth.signOut();
    }

    @After
    public void tearDown() {
        auth.signOut();
    }

    @Test
    public void testSignUpUser() throws Exception {
        String uniqueUsername = "testuser" + System.currentTimeMillis();
        String password = "password123";
        Task<Void> signUpTask = userDAO.signUpUser(uniqueUsername, password);
        Tasks.await(signUpTask, 30, TimeUnit.SECONDS);
        assertTrue(signUpTask.isSuccessful());
    }

    @Test
    public void testSignInUser() throws Exception {
        String uniqueUsername = "testuser" + System.currentTimeMillis();
        String password = "password123";
        Tasks.await(userDAO.signUpUser(uniqueUsername, password), 30, TimeUnit.SECONDS);
        auth.signOut();
        Task<Void> signInTask = userDAO.signInUser(uniqueUsername, password);
        Tasks.await(signInTask, 30, TimeUnit.SECONDS);
        assertTrue(signInTask.isSuccessful());
    }

    @Test
    public void testCheckUserExists() throws Exception {
        String uniqueUsername = "testuser" + System.currentTimeMillis();
        String password = "password123";
        Tasks.await(userDAO.signUpUser(uniqueUsername, password), 30, TimeUnit.SECONDS);
        Task<Boolean> existsTask = userDAO.checkUserExists(uniqueUsername);
        Boolean exists = Tasks.await(existsTask, 30, TimeUnit.SECONDS);
        assertTrue(exists);
    }

    @Test
    public void testChangePassword() throws Exception {
        String uniqueUsername = "testuser" + System.currentTimeMillis();
        String password = "password123";
        Tasks.await(userDAO.signUpUser(uniqueUsername, password), 30, TimeUnit.SECONDS);
        FirebaseUser currentUser = auth.getCurrentUser();
        assertNotNull(currentUser);
        String newPassword = "newpassword123";
        User user = new User(currentUser.getUid(), uniqueUsername, uniqueUsername.toLowerCase() + "@example.com", password);
        Task<Void> changePasswordTask = userDAO.changePassword(user, newPassword);
        Tasks.await(changePasswordTask, 30, TimeUnit.SECONDS);
        assertTrue(changePasswordTask.isSuccessful());
        auth.signOut();
        Task<Void> signInTask = userDAO.signInUser(uniqueUsername, newPassword);
        Tasks.await(signInTask, 30, TimeUnit.SECONDS);
        assertTrue(signInTask.isSuccessful());
    }

    @Test
    public void testResetPassword() throws Exception {
        String uniqueUsername = "testuser" + System.currentTimeMillis();
        String password = "password123";
        Tasks.await(userDAO.signUpUser(uniqueUsername, password), 30, TimeUnit.SECONDS);
        String newPassword = "resetpassword123";
        Task<Void> resetTask = userDAO.resetPassword(uniqueUsername, newPassword);
        Tasks.await(resetTask, 30, TimeUnit.SECONDS);
        assertTrue(resetTask.isSuccessful());
        auth.signOut();
        Task<Void> signInTask = userDAO.signInUser(uniqueUsername, newPassword);
        Tasks.await(signInTask, 30, TimeUnit.SECONDS);
        assertTrue(signInTask.isSuccessful());
    }

    @Test
    public void testGetCurrentUserProfile() throws Exception {
        String uniqueUsername = "testuser" + System.currentTimeMillis();
        String password = "password123";
        Tasks.await(userDAO.signUpUser(uniqueUsername, password), 30, TimeUnit.SECONDS);
        Task<User> profileTask = userDAO.getCurrentUserProfile();
        User userProfile = Tasks.await(profileTask, 30, TimeUnit.SECONDS);
        assertNotNull(userProfile);
        assertEquals(uniqueUsername, userProfile.getUsername());
    }

    @Test
    public void testChangeUsername() throws Exception {
        String uniqueUsername = "testuser" + System.currentTimeMillis();
        String password = "password123";
        Tasks.await(userDAO.signUpUser(uniqueUsername, password), 30, TimeUnit.SECONDS);
        String newUsername = uniqueUsername + "new";
        Task<Void> changeUsernameTask = userDAO.changeUsername(newUsername);
        Tasks.await(changeUsernameTask, 30, TimeUnit.SECONDS);
        assertTrue(changeUsernameTask.isSuccessful());
        Task<User> getUserTask = userDAO.getUserByUsername(newUsername);
        User updatedUser = Tasks.await(getUserTask, 30, TimeUnit.SECONDS);
        assertNotNull(updatedUser);
        assertEquals(newUsername, updatedUser.getUsername());
    }

    @Test
    public void testFollowRequests() throws Exception {
        String requesterUsername = "requester" + System.currentTimeMillis();
        String targetUsername = "target" + System.currentTimeMillis();
        String password = "password123";

        Tasks.await(userDAO.signUpUser(requesterUsername, password), 30, TimeUnit.SECONDS);
        FirebaseUser requesterUser = auth.getCurrentUser();
        assertNotNull(requesterUser);
        String requesterId = requesterUser.getUid();

        auth.signOut();
        Tasks.await(userDAO.signUpUser(targetUsername, password), 30, TimeUnit.SECONDS);
        FirebaseUser targetUser = auth.getCurrentUser();
        assertNotNull(targetUser);
        User targetProfile = Tasks.await(userDAO.getUserByUsername(targetUsername), 30, TimeUnit.SECONDS);
        String targetId = targetProfile.getUserId();

        auth.signOut();
        Tasks.await(userDAO.signInUser(requesterUsername, password), 30, TimeUnit.SECONDS);
        Task<Void> requestFollowTask = userDAO.requestFollow(requesterId, targetId);
        Tasks.await(requestFollowTask, 30, TimeUnit.SECONDS);
        assertTrue(requestFollowTask.isSuccessful());

        Task<Void> acceptTask = userDAO.acceptFollowRequest(requesterId, targetId);
        Tasks.await(acceptTask, 30, TimeUnit.SECONDS);
        assertTrue(acceptTask.isSuccessful());

        Task<Void> unfollowTask = userDAO.unfollowUser(requesterId, targetId);
        Tasks.await(unfollowTask, 30, TimeUnit.SECONDS);
        assertTrue(unfollowTask.isSuccessful());

        Task<Void> anotherRequestTask = userDAO.requestFollow(requesterId, targetId);
        Tasks.await(anotherRequestTask, 30, TimeUnit.SECONDS);
        Task<Void> rejectTask = userDAO.rejectFollowRequest(requesterId, targetId);
        Tasks.await(rejectTask, 30, TimeUnit.SECONDS);
        assertTrue(rejectTask.isSuccessful());
    }

    @Test
    public void testSearchUsers() throws Exception {
        String searcherUsername = "searcher" + System.currentTimeMillis();
        String password = "password123";
        Tasks.await(userDAO.signUpUser(searcherUsername, password), 30, TimeUnit.SECONDS);
        String targetUsername = "searchTarget" + System.currentTimeMillis();
        auth.signOut();
        Tasks.await(userDAO.signUpUser(targetUsername, password), 30, TimeUnit.SECONDS);
        auth.signOut();
        Tasks.await(userDAO.signInUser(searcherUsername, password), 30, TimeUnit.SECONDS);
        String searchPrefix = targetUsername.substring(0, 6);
        Task<List<User>> searchTask = userDAO.searchUsers(searchPrefix);
        List<User> results = Tasks.await(searchTask, 30, TimeUnit.SECONDS);
        for (User user : results) {
            assertNotEquals(searcherUsername, user.getUsername());
        }
    }

    @Test
    public void testGetUserByUsername() throws Exception {
        String uniqueUsername = "testuser" + System.currentTimeMillis();
        String password = "password123";
        Tasks.await(userDAO.signUpUser(uniqueUsername, password), 30, TimeUnit.SECONDS);
        Task<User> getUserTask = userDAO.getUserByUsername(uniqueUsername);
        User user = Tasks.await(getUserTask, 30, TimeUnit.SECONDS);
        assertNotNull(user);
        assertEquals(uniqueUsername, user.getUsername());
    }
}
