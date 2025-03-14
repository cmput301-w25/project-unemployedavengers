/*
 * MoodEvent - Model class representing a mood event.
 *
 * Purpose:
 * - Encapsulates details of a mood event (mood, image URI, reason, trigger, situation, radioSituation, time, existed flag, id).
 * - Provides getters and setters for these fields.
 */
package com.example.unemployedavengers.models;

import android.net.Uri;

import java.io.Serializable;

/**
 * This is a class that represents MoodEvent Objects
 */
public class MoodEvent implements Serializable {
    //Attribute
    private String mood;
    private String imageUri;
    private String reason;
    private String trigger;
    private String situation;
    private String radioSituation;
    private long time;
    private boolean existed;

    private String id;

    private boolean publicStatus;

    //Constructor
    public MoodEvent(String mood, String reason, String trigger, String situation, long time, String radioSituation, String imageUri, boolean publicStatus) {
        this.mood = mood;
        this.reason = reason;
        this.trigger = trigger;
        this.situation = situation;
        this.time = time;
        this.imageUri = imageUri;
        this.radioSituation = radioSituation;
        this.publicStatus = publicStatus;
    }

    public MoodEvent(){
        //empty constructor for firebase
    }

    //getters and setters (exclude time)
    public String getMood() {
        return mood;
    }

    public String getReason() {
        return reason;
    }
    public String getTrigger(){
        return trigger;
    }

    public String getSituation(){
        return situation;
    }

    public long getTime() {
        return time;
    }

    public boolean getExisted(){
        return existed;
    }

    public String getId(){
        return id;
    }

    public String getImageUri() {
        return imageUri;
    }

    public String getRadioSituation() {
        return radioSituation;
    }

    public Boolean getPublicStatus(){return publicStatus;}



    public void setMood(String mood){
        this.mood = mood;
    }
    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setTrigger(String trigger){
        this.trigger = trigger;
    }

    public void setSituation(String situation){
        this.situation = situation;
    }

    public void setExisted(boolean existed) {
        this.existed = existed;
    }

    public void setId(String id) {
        this.id = id;
    }
    public void setRadioSituation(String radioSituation) {
        this.radioSituation = radioSituation;
    }
    public void setImageUri(String imageUri) {
        this.imageUri = imageUri;
    }

    public void setPublicStatus(boolean publicStatus) {
        this.publicStatus = publicStatus;
    }
}
