package com.example.unemployedavengers;

import android.app.Application;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Firebase will use production environment by default
    }
}