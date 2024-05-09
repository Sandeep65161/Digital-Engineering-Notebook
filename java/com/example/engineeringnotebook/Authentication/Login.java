package com.example.engineeringnotebook.Authentication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.engineeringnotebook.R;
import com.example.engineeringnotebook.HomeScreen;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class Login extends AppCompatActivity {
    EditText lEmail, lPassword;
    Button login;
    TextView createAcct, forgotPassword, tempAcct;
    FirebaseAuth fAuth;
    FirebaseUser user;
    FirebaseFirestore fStore;
    ProgressBar progressBarLogin;
    //auth State Listener
    private FirebaseAuth.AuthStateListener authStateListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        //title on action bar on register screen
        getSupportActionBar().setTitle("Account Login");

        lEmail = findViewById(R.id.email);
        lPassword = findViewById(R.id.lPassword);
        login = findViewById(R.id.loginBtn);
        createAcct = findViewById(R.id.createAccount);
        forgotPassword = findViewById(R.id.forgotPasword);
        tempAcct = findViewById(R.id.TempUse);

        progressBarLogin = findViewById(R.id.progressBar3);

        fAuth = FirebaseAuth.getInstance();
        fStore = FirebaseFirestore.getInstance();

        //initialize the authentication state listener
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    //user is signed in, handle accordingly
                    handleAuthenticatedUser(user);
                } else {
                    Toast.makeText(Login.this, "User is null", Toast.LENGTH_SHORT).show();
                }
            }
        };

        //handle click of login btn
        login.setOnClickListener(view -> {
            //extract the data entered in login screen fields
            String mEmail = lEmail.getText().toString();
            String mPassword = lPassword.getText().toString();

            //check if fields are empty, if empty, return user to the same screen
            if (mEmail.isEmpty() || mPassword.isEmpty()) {
                Toast.makeText(Login.this, "All fields are Required", Toast.LENGTH_SHORT).show();
                return;
            }

            progressBarLogin.setVisibility(View.VISIBLE);

            fAuth.signInWithEmailAndPassword(mEmail, mPassword)
                    .addOnSuccessListener(authResult -> {
                        // Handle successful login here
                        handleAuthenticatedUser(authResult.getUser());
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            progressBarLogin.setVisibility(View.GONE);
                            if (e instanceof FirebaseAuthInvalidUserException) {
                                //if the user account does not exist or has been disabled
                                Toast.makeText(Login.this, "User account not found or disabled.",
                                        Toast.LENGTH_SHORT).show();
                            } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                                //if the password is incorrect
                                Toast.makeText(Login.this, "Invalid password.", Toast.LENGTH_SHORT).show();
                            } else {
                                //other login failure
                                Toast.makeText(Login.this, "Login Failed" + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });

        //if 'Don't have an account, Sign Up' is clicked, send user to Register screen(activity)
        createAcct.setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), Register.class)));

        //handle click of use without signing up
        tempAcct.setOnClickListener(view -> {
            //create a new anonymous (temporary) acct that allows access to notebook
            // if successful, display message letting user know they have been allowed to use app temporarily
            //send user to main activity
            //display error message on failure
            fAuth.signInAnonymously().addOnSuccessListener(authResult -> {
                Toast.makeText(Login.this, "Logged in temporarily with minimal features",
                        Toast.LENGTH_SHORT).show();
                startActivity(new Intent(getApplicationContext(), HomeScreen.class));
                finish();
            }).addOnFailureListener(e -> {
                Toast.makeText(Login.this, "Error starting!"+e.getMessage(),Toast.LENGTH_SHORT).show();
                finish();
            });

        });

        //handle click of forgot password
        forgotPassword.setOnClickListener(view -> showForgotPassDialog());
    }

    //method to show the "Forgot Password" Dialog
    private void showForgotPassDialog() {
        //create an EditText field to enter the email
        final EditText resetMail = new EditText(this);

        //create an Alert Dialog for password reset
        AlertDialog.Builder resetDialog = new AlertDialog.Builder(this)
                .setTitle("Reset Password?")
                .setMessage("Enter your email to receive reset link:")
                //EditText field for email input
                .setView(resetMail)

                //handle click of positive button - this sends password Reset Link to email
                .setPositiveButton("Reset", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //get user input (email)
                        String email = resetMail.getText().toString().trim();

                        //check if input is empty
                        if (email.isEmpty()) {
                            Toast.makeText(Login.this, "Email field can not be empty!", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //check if the email is registered with Firebase
                        fAuth.fetchSignInMethodsForEmail(email).addOnSuccessListener(new OnSuccessListener<SignInMethodQueryResult>() {
                            @Override
                            public void onSuccess(SignInMethodQueryResult signInMethodQueryResult) {
                                List<String> signInMethods = signInMethodQueryResult.getSignInMethods();
                                if (signInMethods != null && !signInMethods.isEmpty()) {
                                    //if successful, send password reset email (link) &
                                    //display success message
                                    fAuth.sendPasswordResetEmail(email).addOnSuccessListener(new OnSuccessListener<Void>() {
                                        @Override
                                        public void onSuccess(Void aVoid) {
                                            //email sent successfully
                                            Toast.makeText(Login.this,
                                                    "You will receive password reset link shortly",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                        //on failure, display error message
                                    }).addOnFailureListener(new OnFailureListener() {
                                        @Override
                                        public void onFailure(@NonNull Exception e) {
                                            //failed to send email
                                            Toast.makeText(Login.this,
                                                    "Failed to send reset link: " + e.getMessage(),
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    //email is not registered
                                    Toast.makeText(Login.this, "Email is not registered", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                    //user will not like to reset password
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //close Dialog and do nothing
                    }
                });
        //display the Dialog
        resetDialog.create().show();
    }

    // Method to handle the case when the user is authenticated
    private void handleAuthenticatedUser(FirebaseUser user) {
        if (!user.isAnonymous()) {
            //User is not anonymous
            Toast.makeText(Login.this, "Success", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(getApplicationContext(), HomeScreen.class));
            finish();
        } else {
            //User is anonymous
            Toast.makeText(Login.this, "Granted temporary access",
                    Toast.LENGTH_SHORT).show();
        }

    }
}