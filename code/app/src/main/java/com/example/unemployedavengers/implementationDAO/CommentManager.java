/**
 * CommentManager - Manages operations related to comments in Firestore, including adding, fetching,
 * and deleting comments, as well as handling nested replies.
 *
 * Purpose:
 * - Provides methods for adding, fetching, and deleting comments in a Firestore database.
 * - Supports threaded comments by allowing replies to be associated with parent comments.
 * - Manages the update and removal of reply references when deleting comments.
 *
 * Key Methods:
 * - getCommentsForMoodEvent: Fetches comments for a specific mood event, with an option to include replies.
 * - getRepliesForComment: Retrieves replies for a given comment.
 * - addComment: Adds a new comment (either top-level or a reply), and manages replies for parent comments.
 * - deleteComment: Deletes a comment, including handling the deletion of replies if it's a top-level comment.
 *
 * Known Issues:
 * - Deletion of comments can be slow, especially if there are many replies associated with the comment.
 * - There could be race conditions in cases where multiple operations (e.g., adding and deleting comments) are performed concurrently, which may lead to inconsistent data (for example, replyIds may not be properly updated).
 * - The method `deleteComment` assumes replies are only stored in the `replyIds` field. If the data structure changes (e.g., replies stored elsewhere), this code will need to be updated.
 *
 * Design Patterns:
 * - This class follows the Data Access Object (DAO) design pattern, isolating the application logic from the data persistence layer (Firestore).
 * - The operations are asynchronous, using Firebase Tasks to handle the database interactions, allowing for non-blocking UI updates.
 */


package com.example.unemployedavengers.implementationDAO;

import com.example.unemployedavengers.models.Comment;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles operations related to comments in Firestore
 */
public class CommentManager {
    private static final String TAG = "CommentManager";
    private final FirebaseFirestore db;

    public CommentManager() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Get comments for a specific mood event
     *
     * @param moodEventId The ID of the mood event
     * @param includeReplies Whether to include replies in the result
     * @return Task containing list of comments
     */
    public Task<List<Comment>> getCommentsForMoodEvent(String moodEventId, boolean includeReplies) {
        Query query;
        if (includeReplies) {
            // Get all comments for the mood event
            query = db.collection("comments")
                    .whereEqualTo("moodEventId", moodEventId)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        } else {
            // Get only top-level comments
            query = db.collection("comments")
                    .whereEqualTo("moodEventId", moodEventId)
                    .whereEqualTo("parentId", null)
                    .orderBy("timestamp", Query.Direction.DESCENDING);
        }

        return query.get().continueWith(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }

            List<Comment> comments = new ArrayList<>();
            for (QueryDocumentSnapshot document : task.getResult()) {
                Comment comment = document.toObject(Comment.class);
                comment.setId(document.getId());
                comments.add(comment);
            }

            return comments;
        });
    }

    /**
     * Get replies for a specific comment
     *
     * @param parentId The ID of the parent comment
     * @return Task containing list of reply comments
     */
    public Task<List<Comment>> getRepliesForComment(String parentId) {
        return db.collection("comments")
                .whereEqualTo("parentId", parentId)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    List<Comment> replies = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        Comment reply = document.toObject(Comment.class);
                        reply.setId(document.getId());
                        replies.add(reply);
                    }

                    return replies;
                });
    }

    /**
     * Add a new comment to a mood event
     *
     * @param comment The comment to add
     * @return Task for the operation
     */
    public Task<Void> addComment(Comment comment) {
        CollectionReference commentsRef = db.collection("comments");

        return commentsRef.add(comment)
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }

                    DocumentReference commentRef = task.getResult();
                    comment.setId(commentRef.getId());

                    // If this is a reply, update the parent comment's replyIds
                    if (comment.getParentId() != null) {
                        DocumentReference parentRef = commentsRef.document(comment.getParentId());
                        return parentRef.get().continueWithTask(parentTask -> {
                            if (!parentTask.isSuccessful()) {
                                throw parentTask.getException();
                            }

                            Comment parentComment = parentTask.getResult().toObject(Comment.class);
                            if (parentComment != null) {
                                parentComment.addReplyId(commentRef.getId());
                                return parentRef.update("replyIds", parentComment.getReplyIds());
                            }

                            return Tasks.forResult(null);
                        });
                    }

                    return Tasks.forResult(null);
                });
    }

    /**
     * Delete a comment (and its replies if it's a top-level comment)
     *
     * @param commentId The ID of the comment to delete
     * @return Task for the operation
     */
    public Task<Void> deleteComment(String commentId) {
        DocumentReference commentRef = db.collection("comments").document(commentId);

        return commentRef.get().continueWithTask(task -> {
            if (!task.isSuccessful()) {
                throw task.getException();
            }

            Comment comment = task.getResult().toObject(Comment.class);
            if (comment == null) {
                return null;
            }

            // If it has replies, delete them first
            if (comment.getReplyIds() != null && !comment.getReplyIds().isEmpty()) {
                List<Task<Void>> deleteTasks = new ArrayList<>();
                for (String replyId : comment.getReplyIds()) {
                    deleteTasks.add(db.collection("comments").document(replyId).delete());
                }

                // Wait for all reply deletions to complete, then delete the parent comment
                return Tasks.whenAll(deleteTasks).continueWithTask(t -> commentRef.delete());
            }

            // If it's a reply, update the parent's replyIds
            if (comment.getParentId() != null) {
                DocumentReference parentRef = db.collection("comments").document(comment.getParentId());
                return parentRef.get().continueWithTask(parentTask -> {
                    if (!parentTask.isSuccessful() || !parentTask.getResult().exists()) {
                        // Parent doesn't exist, just delete the comment
                        return commentRef.delete();
                    }

                    Comment parentComment = parentTask.getResult().toObject(Comment.class);
                    if (parentComment != null && parentComment.getReplyIds() != null) {
                        parentComment.getReplyIds().remove(commentId);
                        return parentRef.update("replyIds", parentComment.getReplyIds())
                                .continueWithTask(t -> commentRef.delete());
                    }

                    return commentRef.delete();
                });
            }

            // Simple case: no replies, not a reply
            return commentRef.delete();
        });
    }
}