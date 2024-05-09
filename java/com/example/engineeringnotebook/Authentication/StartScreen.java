package com.example.engineeringnotebook.Authentication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.engineeringnotebook.R;
import com.example.engineeringnotebook.HomeScreen;
import com.google.firebase.auth.FirebaseAuth;

public class StartScreen extends AppCompatActivity {
    FirebaseAuth fAuth;

    //TODO implement logic to identify
    // which user is anonymous
    // which user has an account
    // which user is logged in
    //displayed for 3 secs, sends user to home screen
    //if user is logged in already, send to home screen to create or load notebook
    //if app is opened for first time, register anonymous acct using firebase auth.
    //login user with anon acct and send to home screen
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_screen);

        //create instance of firebase auth.
        fAuth = FirebaseAuth.getInstance();

        //add handler that sends user to Homescreen after some time (delay for 2 secs)
        //finish erases caches
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            //check if user is logged in
            if(fAuth.getCurrentUser() != null) {
                startActivity(new Intent(getApplicationContext(), HomeScreen.class));
                finish();
                //else if user is not registered or logged in
                //create a new anonymous (temporary) acct that allows access to notebook
                // if successful, display message letting user know they have been allowed to use app temporarily
                //send user to home screen
            } else {
                startActivity(new Intent(getApplicationContext(), HomeScreen.class));
                //display error message on failure
                fAuth.signInAnonymously().addOnSuccessListener(authResult -> {
                    Toast.makeText(StartScreen.this, "Logged in temporarily with minimal features", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(getApplicationContext(), HomeScreen.class));
                    finish();
                }).addOnFailureListener(e -> {
                    Toast.makeText(StartScreen.this, "Error starting!"+e.getMessage(),Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }, 2000);
    }
}
