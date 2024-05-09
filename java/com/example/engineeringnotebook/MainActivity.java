package com.example.engineeringnotebook;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.engineeringnotebook.Authentication.Login;
import com.example.engineeringnotebook.Authentication.Register;
import com.example.engineeringnotebook.DigitalInk.DrawingView;
import com.example.engineeringnotebook.DigitalInk.StrokeManager;
import com.example.engineeringnotebook.Model.InformationPage;
import com.example.engineeringnotebook.Model.Notebook;
import com.example.engineeringnotebook.Model.SharedNbStructure;
import com.example.engineeringnotebook.Notebook.Guidelines;
import com.example.engineeringnotebook.Notebook.InfoPageActivity;
import com.example.engineeringnotebook.Notebook.TableOfContentsActivity;
import com.example.engineeringnotebook.utils.LogHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.SignInMethodQueryResult;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {
    private static final String TAG = LogHelper.makeLogTag(MainActivity.class);
    private DrawerLayout drawerLayout;
    private ActionBarDrawerToggle toggle;
    private NavigationView nav_view;


    private FirebaseFirestore fStore = null;
    //for user authentication
    private FirebaseAuth fAuth = null;
    private FirebaseUser user = null;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private Button goToTableContentsBtn, guideLinesBtn, infoPageBtn, signOff, shareBtn, closeBtn;

    private String infoID;
    private boolean contentInDb = false;
    private AlertDialog shareDialog, signOffDialog;
    private Toolbar toolbar;

    // Create a new notebook document in Firestore with the provided title, date, and time
    private String currentDate, currentTime, sharedDate;
    private String currentNotebookID = "", nbookName = "", ownerID = "", sharedNotebookPermission = null;
    private Intent dataIntent;
    private String userId;
    private String isNotebookLocked = null;

    @VisibleForTesting
    final StrokeManager strokeManager = new StrokeManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = findViewById(R.id.toolbar);
        goToTableContentsBtn = findViewById(R.id.goToTableContentsBtn);
        guideLinesBtn = findViewById(R.id.guidelinesBtn);
        infoPageBtn = findViewById(R.id.infoPageBtn);
        signOff = findViewById(R.id.signOff);
        shareBtn = findViewById(R.id.shareBtn);
        closeBtn = findViewById(R.id.closeBtn);
        setSupportActionBar(toolbar);
        nbookName = this.getIntent().getStringExtra("notebookName");
        //getSupportActionBar().setTitle(nbookName);
        Objects.requireNonNull(getSupportActionBar()).setTitle(nbookName);

        //create an instance of fireStore
        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        // firebaseStorage.getReferenceFromUrl("gs://digital-engineering-note-43313.appspot.com");
        storageReference = firebaseStorage.getReference();
        user = fAuth.getCurrentUser();

        // get data from intent
        dataIntent = getIntent();

        if (dataIntent != null) {
            //retrieve notebook id, name from intent
            currentNotebookID = dataIntent.getStringExtra("notebookId");
            ownerID = dataIntent.getStringExtra("ownerID");
            sharedNotebookPermission = dataIntent.getStringExtra("permissions");

            if (ownerID != null) {
                // set userID to ownerID for shared notebooks to get notebook data - shared notebook
                userId = ownerID;

                // check for permissions level ---viewer or editor
                if (sharedNotebookPermission != null) {
                    //good
                    if (sharedNotebookPermission.matches("Viewer")) {
                        // shared notebook viewer level access
                        //shareBtn.setEnabled(false);
                        //signOff.setEnabled(false);

                    } else {
                        // shared notebook Editor level access
                        //shareBtn.setEnabled(false);
                        //signOff.setEnabled(false);
                    }
                }
                Log.w(TAG, "NoteBook id = " + currentNotebookID + "ownerID = " + ownerID);
            } else {
                // default user-- current user stuff
                userId = user.getUid();
            }
            Log.w(TAG, "NoteBook id = " + currentNotebookID + ": userID = " + userId);
        } else {
            // default user
            userId = user.getUid();
            Log.w(TAG, "No NoteBook data passed. ");
        }

        // get notebook lock status
        getNotebookLockStatus(userId, currentNotebookID);

        infoPageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the current notebook ID from the intent
                //String currentNotebookID = tableIntent.getStringExtra("notebookId");
                // Get the user's UID
                //String userId = fAuth.getCurrentUser().getUid();

                // Create a reference to the notebookInfo collection for the user and notebook
                CollectionReference notebookInfoRef = fStore.collection("notebookInfo")
                        .document(userId)
                        .collection(currentNotebookID);

                // Checks if an infoID already exists
                Query infoIDQuery = notebookInfoRef.limit(1);

                infoIDQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            if (!task.getResult().isEmpty()) {
                                // An infoID already exists, use it
                                DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                                String existingInfoID = doc.getId();

                                Intent intent = new Intent(MainActivity.this, InfoPageActivity.class);
                                intent.putExtra("ownerID", userId);
                                intent.putExtra("notebookId", currentNotebookID);
                                intent.putExtra("infoID", existingInfoID);
                                intent.putExtra("isLocked", isNotebookLocked);
                                intent.putExtra("permissions", sharedNotebookPermission);
                                startActivity(intent);
                            } else {
                                // No infoID exists. Create a new one
                                InformationPage informationPage = new InformationPage();
                                DocumentReference newInfoDocRef = notebookInfoRef.document();
                                String newInfoID = newInfoDocRef.getId();

                                newInfoDocRef.set(informationPage).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        Toast.makeText(MainActivity.this, "Info Page Created", Toast.LENGTH_SHORT).show();
                                        contentInDb = true;
                                        Intent intent = new Intent(MainActivity.this, InfoPageActivity.class);
                                        intent.putExtra("ownerID", userId);
                                        intent.putExtra("notebookId", currentNotebookID);
                                        intent.putExtra("infoID", newInfoID);
                                        intent.putExtra("permissions", sharedNotebookPermission);
                                        startActivity(intent);
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Toast.makeText(MainActivity.this, "Failed to Create Info Page", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            Toast.makeText(MainActivity.this, "Error checking infoID", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });

        guideLinesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, Guidelines.class));
            }
        });

        goToTableContentsBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, TableOfContentsActivity.class);
                intent.putExtra("notebookName", nbookName);
                intent.putExtra("notebookId", currentNotebookID);
                intent.putExtra("ownerID", ownerID);
                intent.putExtra("permissions", sharedNotebookPermission);
                startActivity(intent);
            }
        });

        signOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sharedNotebookPermission != null) {
                    // check for permissions level ---viewer or editor
                    //good
                    if (sharedNotebookPermission.matches("Viewer")) {
                        // shared notebook viewer level access
                        Toast.makeText(MainActivity.this, "Owner Access Only", Toast.LENGTH_SHORT).show();

                    } else {
                        // shared notebook Editor level access
                        Toast.makeText(MainActivity.this, "Owner Access only", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // default behavior for current user
                    // check if notebook is not locked
                    if (isNotebookLocked.matches("No")) {
                        View signOffView = getLayoutInflater().inflate(R.layout.sign_off_lock_dialog, null);
                        Button clearCanvasBtn = signOffView.findViewById(R.id.button_clear);
                        DrawingView drawingView = signOffView.findViewById(R.id.drawing_view);
                        drawingView.setStrokeMang(strokeManager);

                        clearCanvasBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                strokeManager.reset();
                                drawingView.clear();
                            }
                        });

                        // build the dialog
                        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                        builder.setView(signOffView);
                        builder.setTitle("Input Signature and Lock");
                        builder.setPositiveButton("Lock", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                // change this notebook lock status to Locked
                                // save signature and add date locked
                                //create bitmap from canvas to save as signature
                                final Bitmap bitmap = getBitmapFromViewUsingCanvas(drawingView);

                                // check if bitmap/signature is not empty
                                lockNotebook(userId, currentNotebookID, bitmap);

                            }
                        });

                        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // Dismiss the dialog when the Cancel button is clicked
                                dialog.dismiss();
                            }
                        });

                        signOffDialog = builder.create();
                        signOffDialog.show();
                    } else {
                        // this notebook is locked
                        Toast.makeText(MainActivity.this, "Notebook is Locked and Signed.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user.isAnonymous()){
                    Toast.makeText(MainActivity.this, "Sync Account to ccess feature", Toast.LENGTH_SHORT).show();
                } else {

                    if (sharedNotebookPermission != null) {
                        // check for permissions level ---viewer or editor
                        //good
                        if (sharedNotebookPermission.matches("Viewer")) {
                            // shared notebook viewer level access
                            Toast.makeText(MainActivity.this, "Owner Access Only", Toast.LENGTH_SHORT).show();

                        } else {
                            // shared notebook Editor level access
                            Toast.makeText(MainActivity.this, "Owner Access Only", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        // default behavior for current user

                        // check if notebook is not locked
                        if (isNotebookLocked.matches("No")) {

                            View popUpView = getLayoutInflater().inflate(R.layout.popup_share, null);
                            EditText emailEditText = popUpView.findViewById(R.id.emailEditText);
                            Spinner roleSpinner = popUpView.findViewById(R.id.roleSpinner);

                            // Create an ArrayAdapter to populate the role Spinner with options
                            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(MainActivity.this,
                                    R.array.roles_array, android.R.layout.simple_spinner_item);
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                            roleSpinner.setAdapter(adapter);

                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setView(popUpView);
                            builder.setTitle("Share via Email");

                            builder.setPositiveButton("Share", null); // We set this to null for now

                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Dismiss the dialog when the Cancel button is clicked
                                    dialog.dismiss();
                                }
                            });

                            shareDialog = builder.create();

                            shareDialog.setOnShowListener(new DialogInterface.OnShowListener() {
                                @Override
                                public void onShow(DialogInterface dialog) {
                                    Button positiveButton = shareDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                                    positiveButton.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            // clicked the share button
                                            String email = emailEditText.getText().toString().trim(); // Trim to remove leading/trailing whitespace
                                            //change email to lowercase
                                            email = email.toLowerCase();
                                            String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";
                                            if (email.isEmpty()) {
                                                Toast.makeText(MainActivity.this, "Email field cannot be empty", Toast.LENGTH_SHORT).show();
                                            } else if (!email.matches(emailPattern)) {
                                                Toast.makeText(MainActivity.this, "Invalid email address", Toast.LENGTH_SHORT).show();
                                            } else {
                                                //convert selection to string
                                                String selectedRole = roleSpinner.getSelectedItem().toString();

                                                if ("Viewer".equals(selectedRole)) {
                                                    //get the email entered by the owner
                                                    String recipientEmail = email;
                                                    String[] parts = recipientEmail.split("@");
                                                    //Log.w(TAG, "email = " + parts[0]);

                                                    //check if the user with the entered email exists
                                                    fAuth.getInstance().fetchSignInMethodsForEmail(recipientEmail)
                                                            .addOnCompleteListener(new OnCompleteListener<SignInMethodQueryResult>() {
                                                                @Override
                                                                public void onComplete(@NonNull Task<SignInMethodQueryResult> task) {
                                                                    if (task.isSuccessful()) {
                                                                        SignInMethodQueryResult result = task.getResult();
                                                                        List<String> signInMethods = result.getSignInMethods();
                                                                        //if the task is successful and the email exists, there will be sign-in methods
                                                                        if (signInMethods != null && !signInMethods.isEmpty()) {

                                                                            //email is registered; //get UID
                                                                            String recipientUID = parts[0];

                                                                            //share the notebook using recipientUID
                                                                            currentDate = dataIntent.getStringExtra("dateCreated");
                                                                            sharedDate = getCurrentDate();

                                                                            FirebaseDatabase database = FirebaseDatabase.getInstance();
                                                                            DatabaseReference sharedNotebooksRef = database.getReference("SharedNotebooks");

                                                                            String sharedNotebookKey = sharedNotebooksRef.push().getKey();
                                                                            SharedNbStructure sharedNotebook = new SharedNbStructure(nbookName, recipientEmail, sharedNotebookKey, currentNotebookID, userId, currentDate, sharedDate, selectedRole);

                                                                            //store the shared notebook details in the Realtime Database using recipientUID
                                                                            sharedNotebooksRef
                                                                                    .child(recipientUID) //recipient UID
                                                                                    .child(sharedNotebookKey) //Unique key for shared notebook entry
                                                                                    .setValue(sharedNotebook);

                                                                            Toast.makeText(MainActivity.this, "Notebook Shared", Toast.LENGTH_SHORT).show();
                                                                            Log.w(TAG, "date NB created = " + currentDate);
                                                                        } else {
                                                                            //Email is not registered; throw an error message to owner
                                                                            Toast.makeText(MainActivity.this, "Email entered is not registered.", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    } else {
                                                                        //handle the task failure
                                                                        Toast.makeText(MainActivity.this, "Unable to check email registration.", Toast.LENGTH_SHORT).show();
                                                                    }

                                                                }
                                                            });
                                                } else if ("Editor".equals(selectedRole)) {
                                                    //get the email entered by the owner
                                                    String recipientEmail = email;
                                                    String[] parts = recipientEmail.split("@");

                                                    //check if the user with the entered email exists
                                                    fAuth.fetchSignInMethodsForEmail(recipientEmail)
                                                            .addOnCompleteListener(task -> {
                                                                if (task.isSuccessful()) {
                                                                    //if the task is successful and the email exists, there will be sign-in methods
                                                                    if (task.getResult() != null && task.getResult().getSignInMethods() != null && !task.getResult().getSignInMethods().isEmpty()) {
                                                                        //email is registered; get the UID of the recipient
                                                                        String recipientUID = parts[0];

                                                                        //continue with sharing the notebook using recipientUID
                                                                        currentDate = dataIntent.getStringExtra("dateCreated");
                                                                        sharedDate = getCurrentDate();

                                                                        FirebaseDatabase database = FirebaseDatabase.getInstance();
                                                                        DatabaseReference sharedNotebooksRef = database.getReference("SharedNotebooks");

                                                                        String sharedNotebookKey = sharedNotebooksRef.push().getKey();
                                                                        SharedNbStructure sharedNotebook = new SharedNbStructure(nbookName, recipientEmail, sharedNotebookKey, currentNotebookID, userId, currentDate, sharedDate, selectedRole);

                                                                        //store the shared notebook details in the Realtime Database using recipientUID
                                                                        sharedNotebooksRef
                                                                                .child(recipientUID) //recipient UID
                                                                                .child(sharedNotebookKey) //Unique key for shared notebook entry
                                                                                .setValue(sharedNotebook);

                                                                        Toast.makeText(MainActivity.this, "Notebook Shared", Toast.LENGTH_SHORT).show();
                                                                        Log.w(TAG, "date NB created = " + currentDate);
                                                                    } else {
                                                                        //Email is not registered, throw an error message to owner
                                                                        Toast.makeText(MainActivity.this, "Email entered is not registered.", Toast.LENGTH_SHORT).show();
                                                                    }
                                                                } else {
                                                                    //Handle the task failure
                                                                    Toast.makeText(MainActivity.this, "Unable to check email registration.", Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                }

                                                shareDialog.dismiss();
                                            }
                                        }
                                    });
                                }
                            });

                            shareDialog.show();
                        } else {
                            // notebook is locked
                            Toast.makeText(MainActivity.this, "Notebook is Locked and signed.", Toast.LENGTH_SHORT).show();

                        }
                    }
                }
            }
        });

        //on click of close, go to home screen
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(MainActivity.this, HomeScreen.class));
            }
        });

        //find DrawerLayout with id drawer in activity_main layout assign it to the variable drawerLayout
        //DrawerLayout implements a sliding navigation drawer shows a navigation menu when
        //user swipes from the left edge of the screen or taps on hamburger icon
        drawerLayout = findViewById(R.id.drawer);
        //NavigationView provides navigation menu to be used with a DrawerLayout.
        //it shows a list of items or options that users can select from navigation drawer
        nav_view = findViewById(R.id.nav_view);
        nav_view.setNavigationItemSelectedListener(this);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.open, R.string.close);
        drawerLayout.addDrawerListener(toggle);
        //enables hamburger sign in toolbar
        toggle.setDrawerIndicatorEnabled(true);
        //informs action bar toggle that navigation drawer is open or closed
        toggle.syncState();

        View headerView = nav_view.getHeaderView(0);
        TextView username = headerView.findViewById(R.id.userDisplayName);
        TextView userEmail = headerView.findViewById(R.id.userDisplayEmail);

        //use textview to set value to be displayed in header
        userEmail.setText(user.getEmail());
        username.setText(user.getDisplayName());

    }

    private void updateNotebookStamp(String currentNotebookID) {
        final String currentDate = getCurrentDate();
        final String currentTime = getCurrentTime();

        // Create a HashMap to update
        Map<String, Object> newStamp = new HashMap<>();
        newStamp.put("lastModifiedTime", currentTime);
        newStamp.put("lastModifiedDate", currentDate);

        try {
            // Structure
            // stamp > userID >> notebookID >> stamp54YgFGl = same document(ID) for all
            fStore.collection("stamp").document(userId)
                    .collection(currentNotebookID)
                    .document("stamp54YgFGl")
                    .update(newStamp)
                    .addOnSuccessListener(documentReference -> {
                        Log.w(TAG, "Successfully updated notebook date stamp");

                    })
                    .addOnFailureListener(e -> {
                        // Handle the case where creating the notebook document failed
                        Log.w(TAG, "Failed to update notebook date stamp" + e.getMessage());
                    });
        } catch (Exception e) {
            Log.w(TAG, e.toString());
        }
    }

    private void lockNotebook(String userId, String currentNotebookID, Bitmap bitmap) {
        // set field isLocked to Yes
        if (userId != null) {
            //retrieve user's notebook data from Firebase
            fStore.collection("users")
                    .document(userId)
                    .collection("notebooks")  //whereEqualTo("id", currentNotebookID)
                    .document(currentNotebookID)
                    .update("isLocked", "Yes")
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void unused) {
                            Log.w(TAG, "Successfully updated notebook lock status in firestore db");
                            // save this notebook signature and date locked
                            // save this signature in storage
                            saveSignatureBitmapToDB(userId, currentNotebookID, bitmap);

                            // set lock date
                            setSignatureDate();

                            //update notebook date stamp
                            updateNotebookStamp(currentNotebookID);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "Error Locking Notebook " + e);
                        }
                    });
        }

    }

    private void setSignatureDate() {
        // Create a reference to the notebookInfo collection for the user and notebook
        Map<String, Object> updatedDate = new HashMap<>();
        updatedDate.put("date", getCurrentDate());

        CollectionReference notebookInfoRef = fStore.collection("notebookInfo")
                .document(userId)
                .collection(currentNotebookID);

        // Checks if an infoID already exists
        Query infoIDQuery = notebookInfoRef.limit(1);

        infoIDQuery.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if (!task.getResult().isEmpty()) {
                        // An infoID already exists, use it
                        DocumentSnapshot doc = task.getResult().getDocuments().get(0);
                        String existingInfoID = doc.getId();

                        DocumentReference notebookRef = fStore.collection("notebookInfo").document(userId)
                                .collection(currentNotebookID).document(existingInfoID);

                        //set locked date
                        notebookRef.update(updatedDate);

                    } else {
                        // No infoID exists. Create a new one
                        Log.d(TAG, "Error saving lock date ");
                    }
                } else {
                    Log.d(TAG, "Error saving lock date ");
                }
            }
        });

    }

    public File saveBitmapToFile(Bitmap bmp) throws IOException {
        ContextWrapper cw = new ContextWrapper(getApplicationContext());
        File directory = cw.getDir("imageDir", Context.MODE_PRIVATE);
        // delete files here first
        if (directory.isDirectory()) {
            String[] children = directory.list();
            for (int i = 0; i < Objects.requireNonNull(children).length; i++) {
                new File(directory , children[i]).delete();
            }
        }
        File file = new File(directory, currentNotebookID + "signature" + ".png");
        if (!file.exists()) {
            Log.d(TAG, file.toString());
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                //bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                bmp.compress(Bitmap.CompressFormat.PNG, 0, fos);
                fos.flush();
                fos.close();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    private void saveSignatureBitmapToDB(String userId, String notebookID, Bitmap bitmap) {
        StorageReference signatureRef = storageReference.child("signatures/" +
                userId + "/" + notebookID + "/sign.png" );

        try {
            File newfile = saveBitmapToFile(bitmap);
            Uri imageUri = Uri.fromFile(newfile);
            Log.w(TAG, "file: " + imageUri);
            InputStream stream = new FileInputStream(newfile);

            UploadTask uploadTask = signatureRef.putStream(stream);
            uploadTask.addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception exception) {
                    // Handle unsuccessful uploads
                    Log.w(TAG, "Failed uploading signature ");
                }
            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                    Toast.makeText(MainActivity.this, "Success uploading signature", Toast.LENGTH_SHORT).show();
                    Log.w(TAG, "Success uploading signature ");
                }
            });

        } catch (IOException e) {
            Log.d(TAG, "Bad error: " + e);
        }

    }


    //handling click of 'Notes',  need to implement the interface from nav view
    //hence 'implements NavigationView.OnNavigationItemSelectedListener'
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        //check which item is clicked and based on that, display message or start new activity
        switch (item.getItemId())
        {

            //if user clicks on Sync Note, send to Login screen
            case R.id.sync:
                if (user.isAnonymous()){
                    displayAlert();
                } else {
                    Toast.makeText(this, "Account already connected", Toast.LENGTH_SHORT).show();
                }
                break;

            //if user clicks on load Notebook, send to load notebook activity
            case R.id.loadNoteBook:
                startActivity(new Intent(this, LoadNotebook.class));
                break;

            //if user clicks on Create New Notebook, send to create notebook activity
            case R.id.createNewNoteBook:
                newNotebookDialog();
                break;

            //if user clicks on Add Note, send to HomeScreen
            case R.id.sharedNbooks:
                startActivity(new Intent(this, SharedNotebooks.class));
                break;

            //if user clicks on logout, check type of user and send to login screen
            case R.id.logout:
                checkUser();
                break;
            default:
                Toast.makeText(this, "coming soon.", Toast.LENGTH_SHORT).show();
        }
        //close navigation drawer on options selected
        drawerLayout.closeDrawer(GravityCompat.START);
        return false;
    }

    //method will check if user is logged in with an acct or is a temporary user
    private void checkUser() {
        //if user is a temporary user
        //warn user of data deletion if email isn't connected to session
        if (fAuth != null && user != null) {
            if (user.isAnonymous()) {
                displayAlert();
            } else {
                //else if user is logged in with an account, send user back to login screen
                // sign user out and redirect to login screen
                fAuth.signOut();

                Intent intent = new Intent(getApplicationContext(), Login.class);
                startActivity(intent);
            }
        } else {
            Toast.makeText(getApplicationContext(), "Failed to Logout", Toast.LENGTH_SHORT).show();
        }
    }

    //method will display warning to temporary user trying to logout
    private void displayAlert() {
        AlertDialog.Builder warning = new AlertDialog.Builder(this)
                .setTitle("Are you sure?")
                .setMessage("Logged in as guest user. Logging out will delete all note entries.")
                //temporary user will like to create acct and sync notes
                .setPositiveButton("Sync Note", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startActivity(new Intent(getApplicationContext(), Register.class));
                        finish();
                    }
                    //temporary user will like to logout and lose data
                }).setNegativeButton("Logout", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //delete temp. user and send to Login screen
                        String notebooksCollectionPath = "users/" + user.getUid() + "/notebooks";
                        fStore.collection(notebooksCollectionPath).get()
                                .addOnSuccessListener(queryDocumentSnapshots -> {
                                    //loop through the query results and delete each notebook
                                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                                        document.getReference().delete()
                                                .addOnSuccessListener(unused -> Toast.makeText(MainActivity.this,
                                                        "Notebook deleted successfully", Toast.LENGTH_SHORT).show())
                                                .addOnFailureListener(e -> {
                                                    //handle failure to delete notebook document
                                                    Toast.makeText(MainActivity.this,
                                                            "Error deleting notebook: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                });
                                    }
                                    //after deleting all anonymous user notebooks, delete anonymous user
                                    user.delete().addOnSuccessListener(unused -> {
                                        Toast.makeText(MainActivity.this, "Temporary User & Data Deleted Successfully",
                                                Toast.LENGTH_SHORT).show();
                                        startActivity(new Intent(getApplicationContext(), Login.class));
                                        finish();
                                    }).addOnFailureListener(e -> {
                                        //handle failure to delete user if needed
                                        Toast.makeText(MainActivity.this, "Error Deleting Temporary User: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show();
                                    });
                                })
                                .addOnFailureListener(e -> {
                                    //handle failure to query notebooks for the anonymous user
                                    Toast.makeText(MainActivity.this, "Error fetching notebooks: " + e.getMessage(),
                                            Toast.LENGTH_SHORT).show();
                                });
                    }
                });
        warning.show();
    }
    private void newNotebookDialog() {
        //create an EditTextField to enter notebook title
        final EditText notebookTitle = new EditText(this);
        // set isLocked initially to false
        String isNotebookLocked = "No";

        //create alert dialog for new notebook title input
        AlertDialog.Builder newNbDialog = new AlertDialog.Builder(this)
                .setTitle("Create New Notebook")
                .setMessage("Enter Notebook Title")
                .setView(notebookTitle)

                //handle click of positive button - creates notebook with user entered title
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //extract String passed through editText field
                        String NbookName = notebookTitle.getText().toString().trim();

                        //check if input is empty, if empty display message
                        if (NbookName.isEmpty()){
                            Toast.makeText(MainActivity.this, "Title field can not be empty!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Create a new notebook document in Firestore with the provided title, date, and time
                        currentDate = getCurrentDate();
                        currentTime = getCurrentTime();

                        //create new Notebook object with user provided title (NbookName)
                        //notebook creation date (currentDate), and creation time (currentTime)
                        //assign to newNotebook
                        Notebook newNotebook = new Notebook(NbookName, currentDate, currentTime, isNotebookLocked);

                        // Creates a new document in the "notebooks" collection under the user's UID,
                        // with the notebook data provided by the user as the document's content.
                        // The "users" collection stores data for each user, and each user has a collection of "notebooks".
                        // The notebook data is stored as a custom class called "Notebook" in the "notebooks" collection.
                        // structure in database
                        // Users > userID >> notebooks > notebookID >> notebook info (date created, id, name, time created)
                        fStore.collection("users").document(FirebaseAuth.getInstance()
                                        .getCurrentUser().getUid()).collection("notebooks").add(newNotebook)
                                .addOnSuccessListener(documentReference -> {
                                    // Get the auto-generated ID of the newly created notebook document
                                    String NbookId = documentReference.getId();

                                    // Create a HashMap to store the "id" fields of the notebook
                                    Map<String, Object> notebookData = new HashMap<>();
                                    notebookData.put("id", NbookId);

                                    // Update the notebook document with the "id" and "title" fields
                                    documentReference.update(notebookData)
                                            .addOnSuccessListener(aVoid -> {
                                                // Redirect user to MainActivity with notebook data
                                                Toast.makeText(MainActivity.this, "Notebook created", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(MainActivity.this, MainActivity.class);
                                                intent.putExtra("notebookName", NbookName);
                                                intent.putExtra("notebookId", NbookId);
                                                intent.putExtra("isLocked", isNotebookLocked);
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                // Handle the case where updating the notebook document failed
                                                Toast.makeText(MainActivity.this, "Error creating notebook: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });

                                })
                                .addOnFailureListener(e -> {
                                    // Handle the case where creating the notebook document failed
                                    Toast.makeText(MainActivity.this, "Error creating notebook: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //close Dialog and do nothing
                    }
                });
        newNbDialog.create().show();
    }

    //get date
    private String getCurrentDate() {
        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);

        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        return df.format(c);
    }

    //get time
    private String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int amPm = calendar.get(Calendar.AM_PM);

        String amPmIndicator = (amPm == Calendar.AM) ? "AM" : "PM";

        return hour + ":" + minute + " " + amPmIndicator;
    }

    //to create and display options menu in the activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //to display or handle MENU (settings) option click
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.settings){
            Toast.makeText(this, "setting is clicked", Toast.LENGTH_SHORT).show();
        }
        return super.onOptionsItemSelected(item);
    }

    private Bitmap getBitmapFromViewUsingCanvas(View view) {
        // Create a new Bitmap object with the desired width and height
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);

        // Create a new Canvas object using the Bitmap
        Canvas canvas = new Canvas(bitmap);

        // Draw the View into the Canvas
        view.draw(canvas);

        // Return the resulting Bitmap
        return bitmap;
    }

    private void getNotebookLockStatus(String userId, String currentNotebookID) {
        if (userId != null) {
            //retrieve user's notebook data from Firebase
            fStore.collection("users")
                    .document(userId)
                    .collection("notebooks")  //whereEqualTo("id", currentNotebookID)
                    .document(currentNotebookID)
                    .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.getString("isLocked") != null) {
                                isNotebookLocked = documentSnapshot.getString("isLocked");
                            } else {
                                // older data
                                // so set to No
                                isNotebookLocked = "No";
                            }
                            //Log.w(TAG, "NoteBk isLock = " + isNotebookLocked);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "NoteBk isLock error: " + e);
                        }
                    });
        }
    }

}








