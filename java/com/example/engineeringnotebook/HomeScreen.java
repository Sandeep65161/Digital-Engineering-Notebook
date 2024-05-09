package com.example.engineeringnotebook;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.engineeringnotebook.Authentication.Login;
import com.example.engineeringnotebook.Model.DateStamp;
import com.example.engineeringnotebook.Model.Notebook;
import com.example.engineeringnotebook.utils.LogHelper;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class HomeScreen extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(HomeScreen.class);
    private ImageButton logOut;
    private FloatingActionButton cNote;
    private Button loadNote, sharedNote;
    private FirebaseFirestore fStore = null;
    private FirebaseUser user = null;
    private FirebaseAuth fAuth = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_screen);

        logOut = findViewById(R.id.logoutBtn);
        cNote = findViewById(R.id.createNbookFloat);
        loadNote = findViewById(R.id.loadNotebook);
        sharedNote = findViewById(R.id.sharedNotebooks);

        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();
        user = fAuth.getCurrentUser();

        if (user != null) {
            if (user.isAnonymous()) {
                //if user is anonymous, display guest welcome message on title bar
                getSupportActionBar().setTitle("Welcome, Guest User!");
            } else {
                // If user is logged in, display welcome message for user
                String userName = user.getDisplayName();
                getSupportActionBar().setTitle("Welcome, " + userName + "!");
            }
        }

        //if user clicks on logout, send back to login screen
        logOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user.isAnonymous()){
                    displayAlert();
                } else if (fAuth != null) {
                    // sign user out and redirect to login screen
                    fAuth.signOut();

                    Intent intent = new Intent(getApplicationContext(), Login.class);
                    startActivity(intent);
                }
            }
        });

        //if user clicks on create new note, display message
        cNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newNotebookDialog();
            }
        });

        //if user clicks on load notebooks, start new activity
        loadNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getApplicationContext(), LoadNotebook.class));
            }
        });

        sharedNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (user.isAnonymous()){
                    Toast.makeText(HomeScreen.this, "Sync account to access feature", Toast.LENGTH_SHORT).show();
                } else {
                    startActivity(new Intent(getApplicationContext(), SharedNotebooks.class));
                }
            }
        });
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
                            Toast.makeText(HomeScreen.this, "Title field can not be empty!", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // Create a new notebook document in Firestore with the provided title, date, and time
                        String currentDate = getCurrentDate();
                        String currentTime = getCurrentTime();

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

                                    // create notebook date stamp
                                    doNotebookStamp(FirebaseAuth.getInstance()
                                            .getCurrentUser().getUid(), NbookId);

                                    // Create a HashMap to store the "id" fields of the notebook
                                    Map<String, Object> notebookData = new HashMap<>();
                                    notebookData.put("id", NbookId);

                                    // Update the notebook document with the "id" and "title" fields
                                    documentReference.update(notebookData)
                                            .addOnSuccessListener(aVoid -> {
                                                // Redirect user to MainActivity with notebook data
                                                Toast.makeText(HomeScreen.this, "Notebook created", Toast.LENGTH_SHORT).show();
                                                Intent intent = new Intent(HomeScreen.this, MainActivity.class);
                                                intent.putExtra("notebookName", NbookName);
                                                intent.putExtra("notebookId", NbookId);
                                                intent.putExtra("isLocked", isNotebookLocked);
                                                intent.putExtra("dateCreated", currentDate);
                                                startActivity(intent);
                                                finish();
                                            })
                                            .addOnFailureListener(e -> {
                                                // Handle the case where updating the notebook document failed
                                                Toast.makeText(HomeScreen.this, "Error creating notebook: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });

                                })
                                .addOnFailureListener(e -> {
                                    // Handle the case where creating the notebook document failed
                                    Toast.makeText(HomeScreen.this, "Error creating notebook: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    //method will display warning to temporary user trying to logout
    private void displayAlert() {
        AlertDialog.Builder warning = new AlertDialog.Builder(this)
                .setTitle("Are you sure?")
                .setMessage("Logged in as a guest user. Logging out will delete all notebooks and entries.")
                //temp user will like to stay on screen
                .setPositiveButton("Stay", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        // Do nothing, user chose to stay
                    }
                })
                //temp user will like to logout and lose data
                .setNegativeButton("Logout", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //handle logout and deletion of data
                        deleteTempUserData();
                    }
                });
        warning.show();
    }

    private void doNotebookStamp(String userId, String currentNotebookID) {
        // initial is current date created
        //final String initialDate = "01/01/0000";
        //final String initialTime = "00:00 AM";
        final String currentDate = getCurrentDate();
        final String currentTime = getCurrentTime();

        DateStamp dateStamp = new DateStamp(currentTime, currentDate);
        try {
            // Structure
            // stamp > userID >> notebookID >> stamp54YgFGl = same document(ID) for all
            fStore.collection("stamp").document(userId)
                    .collection(currentNotebookID)
                    .document("stamp54YgFGl")
                    .set(dateStamp)
                    .addOnSuccessListener(documentReference -> {
                        Log.w(TAG, "Created date stamp with id: " + "stamp54YgFGl");

                    })
                    .addOnFailureListener(e -> {
                        // Handle the case where creating the notebook document failed
                        Log.w(TAG, "Failed to create date stamp" + e.getMessage());
                    });
        } catch (Exception e) {
            Log.w(TAG, e.toString());
        }

    }

    //get date
    private String getCurrentDate() {
        /*Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH) + 1;
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        return month + "/" + day + "/" + year;*/
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

    // Method to handle the deletion of data and user logout
    private void deleteTempUserData() {
        // Delete temporary user data and notebooks
        String notebooksCollectionPath = "users/" + user.getUid() + "/notebooks";
        fStore.collection(notebooksCollectionPath).get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    // Loop through the query results and delete each notebook
                    for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                        document.getReference().delete()
                                .addOnSuccessListener(unused -> {
                                    // Handle successful deletion of notebooks
                                    Toast.makeText(HomeScreen.this,
                                            "Notebook deleted successfully", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    // Handle failure to delete notebook document
                                    Toast.makeText(HomeScreen.this,
                                            "Error deleting notebook: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    }
                    // After deleting all anonymous user notebooks, delete the anonymous user
                    user.delete().addOnSuccessListener(unused -> {
                        // Handle successful deletion of temporary user
                        Toast.makeText(HomeScreen.this, "Temporary User & Data Deleted Successfully",
                                Toast.LENGTH_SHORT).show();
                        // Redirect to the login screen
                        startActivity(new Intent(getApplicationContext(), Login.class));
                        finish();
                    }).addOnFailureListener(e -> {
                        // Handle failure to delete user if needed
                        Toast.makeText(HomeScreen.this, "Error Deleting Temporary User: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
                })
                .addOnFailureListener(e -> {
                    // Handle failure to query notebooks for the anonymous user
                    Toast.makeText(HomeScreen.this, "Error fetching notebooks: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }


}