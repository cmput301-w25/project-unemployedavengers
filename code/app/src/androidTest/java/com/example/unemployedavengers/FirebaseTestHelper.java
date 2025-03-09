package com.example.unemployedavengers;

import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;

import java.net.HttpURLConnection;
import java.net.URL;

public class FirebaseTestHelper {
    public static void setupEmulator() {
        // Android emulator uses 10.0.2.2 to access localhost
        String androidLocalhost = "10.0.2.2";
        int portNumber = 8080; // Use the port you configured

        FirebaseFirestore.getInstance().useEmulator(androidLocalhost, portNumber);
    }

    public static void clearDatabase() {
        // Your Firebase project ID
        String projectId = "unemployeedavenger";

        try {
            URL url = new URL("http://10.0.2.2:8080/emulator/v1/projects/" + projectId + "/databases/(default)/documents");
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestMethod("DELETE");
            int response = urlConnection.getResponseCode();
            Log.i("Response Code", "Response Code: " + response);
            urlConnection.disconnect();
        } catch (Exception e) {
            Log.e("ClearDB Error", e.getMessage());
        }
    }
}