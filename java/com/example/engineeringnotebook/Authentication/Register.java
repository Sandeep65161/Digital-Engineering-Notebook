package com.example.engineeringnotebook.Authentication;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.engineeringnotebook.HomeScreen;
import com.example.engineeringnotebook.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class Register extends AppCompatActivity {

    EditText rUserName, rUserEmail, rUserPass, rUserConfirmPass;
    Button createAccount;
    TextView loginAct;
    FirebaseAuth fAuth;
    ProgressBar progressBarRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        //title on action bar on register screen
        getSupportActionBar().setTitle("Account Registration");
        //back button in register screen
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        rUserName = findViewById(R.id.userName);
        rUserEmail = findViewById(R.id.userEmail);
        rUserPass = findViewById(R.id.password);
        rUserConfirmPass = findViewById(R.id.passwordConfirm);
        createAccount = findViewById(R.id.syncAccount);
        loginAct = findViewById(R.id.login);

        progressBarRegister = findViewById(R.id.progressBar4);

        fAuth = FirebaseAuth.getInstance();

        //if 'Login Here' is clicked, send user to login screen(activity)
        loginAct.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), Login.class));
            }
        });

        //handle click on create account btn
        createAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String uUsername = rUserName.getText().toString();
                String uUserEmail = rUserEmail.getText().toString();
                String uUserPass = rUserPass.getText().toString();
                String uConfirmPass = rUserConfirmPass.getText().toString();

                //check if one of the data above is empty display error notifying user all fields are required
                if (uUserEmail.isEmpty() || uUsername.isEmpty() || uUserPass.isEmpty() || uConfirmPass.isEmpty()) {
                    Toast.makeText(Register.this, "All Fields are Required", Toast.LENGTH_SHORT).show();
                    return;
                }

                //check if password and confirm password are the same, if different, display error
                if (!uUserPass.equals(uConfirmPass)) {
                    rUserConfirmPass.setError("Passwords do not match");
                    return;
                }

                progressBarRegister.setVisibility(View.VISIBLE);

                FirebaseUser currentUser = fAuth.getCurrentUser();

                if (currentUser != null && currentUser.isAnonymous()) {
                    //temp user is trying to link data with a temporary account
                    linkGuestWithNewAccount(currentUser, uUserEmail, uUserPass, uUsername);
                } else {
                    //user is creating a new account
                    createNewAccount(uUserEmail, uUserPass, uUsername);
                }
            }
        });
    }

    //handle click on back button
    //on click send user to main activity
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        startActivity(new Intent(this, Login.class));
        finish();
        return super.onOptionsItemSelected(item);
    }

    private void linkGuestWithNewAccount(FirebaseUser currentUser, String email, String password, String username) {
        if (currentUser == null) {
            //handle the case where the created user is null --- *this should not happen*
            Toast.makeText(Register.this, "Current user is null", Toast.LENGTH_SHORT).show();
            return;
        }

        AuthCredential credential = EmailAuthProvider.getCredential(email, password);

        currentUser.linkWithCredential(credential)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        // Data is successfully linked with the new account
                        // Update user profile display name
                        FirebaseUser updatedUser = fAuth.getCurrentUser();
                        if (updatedUser != null) {
                            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();
                            updatedUser.updateProfile(request);

                            // Redirect to home screen
                            Toast.makeText(Register.this, "Data is now synced", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), HomeScreen.class));
                            finish();
                        } else {
                            //handle the case where the created user is null --- *this should not happen*
                            Toast.makeText(Register.this, "Updated user is null", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Failed to link data with the new account
                        Toast.makeText(Register.this, "Failed to connect, Try Again: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBarRegister.setVisibility(View.GONE);
                    }
                });
    }

    //handle creation of new account
    private void createNewAccount(String email, String password, String username) {
        fAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                    @Override
                    public void onSuccess(AuthResult authResult) {
                        //new account created successfully
                        FirebaseUser user = fAuth.getCurrentUser();
                        if (user != null) {
                            UserProfileChangeRequest request = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build();
                            user.updateProfile(request);

                            //redirect user to home screen
                            Toast.makeText(Register.this, "Account created successfully", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(getApplicationContext(), HomeScreen.class));
                            finish();
                        } else {
                            //handle the case where the created user is null --- *this should not happen*
                            Toast.makeText(Register.this, "Created user is null", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed to create a new account
                        Toast.makeText(Register.this, "Account creation failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        progressBarRegister.setVisibility(View.GONE);
                    }
                });
    }

}