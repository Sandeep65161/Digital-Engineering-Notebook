package com.example.engineeringnotebook.Notebook;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.engineeringnotebook.R;

public class Guidelines extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_guidelines);
        //title on action bar on register screen
        getSupportActionBar().setTitle("Guidelines");
        //back button in register screen
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }
    //handle click on back button
    //on click send user to main activity
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }
}