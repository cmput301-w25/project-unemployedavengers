package com.example.unemployedavengers.models;

import java.io.Serializable;

public class User implements Serializable {
    private String userId;
    private String username;
    private String dummyEmail;
    private String password;

    public User() {
    }

    public User(String userId, String username, String dummyEmail, String password) {
        this.userId = userId;
        this.username = username;
        this.dummyEmail = dummyEmail;
        this.password = password;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDummyEmail() {
        return dummyEmail;
    }

    public void setDummyEmail(String dummyEmail) {
        this.dummyEmail = dummyEmail;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}