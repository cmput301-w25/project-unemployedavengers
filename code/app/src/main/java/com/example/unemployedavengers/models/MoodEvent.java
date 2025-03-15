/*
 * MoodEvent - Model class representing a mood event.
 *
 * Purpose:
 * - Encapsulates details of a mood event (mood, image URI, reason, situation, radioSituation, time, existed flag, id, userId).
 * - Provides getters and setters for these fields.
 */
package com.example.unemployedavengers.models;

import java.io.Serializable;

/**
 * This is a class that represents MoodEvent Objects
 */
public class MoodEvent implements Serializable {
    //Attribute
    private String mood;
    private String imageUri;
    private String reason;
    private String situation;
    private String radioSituation;
    private long time;
    private boolean existed;
    private String id;
    private String userId; // Track which user created the mood event
    private boolean publicStatus = true;

    //Constructor
    // Constructor without publicStatus parameter (for backward compatibility)
    public MoodEvent(String mood, String reason, String situation, long time, String radioSituation, String imageUri) {
        this.mood = mood;
        this.reason = reason;
        this.situation = situation;
        this.time = time;
        this.radioSituation = radioSituation;
        this.imageUri = imageUri;
        this.publicStatus = true; // Default to public
    }

    // Constructor with publicStatus parameter
    public MoodEvent(String mood, String reason, String situation, long time, String radioSituation, String imageUri, boolean publicStatus) {
        this.mood = mood;
        this.reason = reason;
        this.situation = situation;
        this.time = time;
        this.radioSituation = radioSituation;
        this.imageUri = imageUri;
        this.publicStatus = publicStatus;
    }

    public MoodEvent(){
        //empty constructor for firebase
    }

    //getters and setters
    public String getMood() {
        return mood;
    }

    public String getReason() {
        return reason;
    }

    public String getSituation(){
        return situation;
    }

    public long getTime() {
        return time;
    }

    public boolean getPublicStatus() {
        return publicStatus;
    }

    public void setPublicStatus(boolean publicStatus) {
        this.publicStatus = publicStatus;
    }

    public boolean getExisted(){
        return existed;
    }

    public String getId(){
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public void setMood(String mood){
        this.mood = mood;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setSituation(String situation){
        this.situation = situation;
    }

    //Exclude time as we will not let user change time.

    public void setExisted(boolean existed) {
        this.existed = existed;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRadioSituation() {
        return radioSituation;
    }

    public void setRadioSituation(String radioSituation) {
        this.radioSituation = radioSituation;
    }

    public String getImageUri() {
        return imageUri;
    }

    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }
}