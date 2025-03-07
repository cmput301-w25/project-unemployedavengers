package com.example.unemployedavengers.models;

import java.io.Serializable;

/**
 * This is a class that represents MoodEvent Objects
 */
public class MoodEvent implements Serializable {
    //Attribute
    private String mood;
    private String trigger;
    private String situation;

    private long time;

    //Constructor
    public MoodEvent(String mood, String trigger, String situation, long time) {
        this.mood = mood;
        this.trigger = trigger;
        this.situation = situation;
        this.time = time;
    }

    //getters and setters
    public String getMood() {
        return mood;
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

    public void setMood(String mood){
        this.mood = mood;
    }

    public void setTrigger(String trigger){
        this.trigger = trigger;
    }

    public void setSituation(String situation){
        this.situation = situation;
    }

    //Exclude time as we will not let user change time.


}
