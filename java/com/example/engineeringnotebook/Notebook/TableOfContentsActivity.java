package com.example.engineeringnotebook.Notebook;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.engineeringnotebook.Model.InitialNote;
import com.example.engineeringnotebook.R;
import com.example.engineeringnotebook.utils.LogHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TableOfContentsActivity extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(TableOfContentsActivity.class);
    private RecyclerView recyclerView;
    private Button createNewEntryBtn;
    private List<InitialNote> initialNoteList = null;
    private EntryRecyclerAdapter entryRecyclerAdapter;
    private FirebaseAuth mAuth = null;
    private FirebaseUser user = null;
    private FirebaseDatabase mDatabase = null;
    private FirebaseFirestore fStore = null;
    private String currentNotebookID = null;
    private String isNotebookLocked = null;
    private String userId = null;
    private String notebookOwnerID = null, sharedNotebookPermission = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_table_of_contents);
        recyclerView = findViewById(R.id.entry_recyclerView);
        createNewEntryBtn = findViewById(R.id.create_new_entry_btn);

        //retrieve notebook name from intent
        //String NbookName = getIntent().getStringExtra("notebookName");
        //Add title to Table of content screen
        getSupportActionBar().setTitle("Table of Contents");
        //adds back button to activity - don't fix with Object.requireNonNull recommendation (causes   bug)
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // init realtime db
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        user = mAuth.getCurrentUser();
        fStore = FirebaseFirestore.getInstance();

        // get data from intent
        Intent tableIntent = getIntent();

        if (tableIntent != null) {
            currentNotebookID = tableIntent.getStringExtra("notebookId");
            Log.w(TAG, "NoteBk id = " + currentNotebookID);

            Log.w(TAG, "now date = " + getCurrentDate());


            notebookOwnerID = tableIntent.getStringExtra("ownerID");
            sharedNotebookPermission = tableIntent.getStringExtra("permissions");

            if (notebookOwnerID != null) {
                // set userID to ownerID for shared notebooks to get notebook data
                userId = notebookOwnerID;
            } else {
                // default user
                userId = user.getUid();
            }
        } else {
            // default user
            userId = user.getUid();
            Log.w(TAG, "Error intent, no NoteBook data. ");
        }

        // get this notebook's current locked status from db
        getNotebookLockStatus(userId, currentNotebookID);

        initialNoteList = new ArrayList<>();
        entryRecyclerAdapter = new EntryRecyclerAdapter(TableOfContentsActivity.this, initialNoteList, currentNotebookID,
                notebookOwnerID, sharedNotebookPermission, isNotebookLocked);
        recyclerView.setLayoutManager(new LinearLayoutManager(TableOfContentsActivity.this));
        recyclerView.setAdapter(entryRecyclerAdapter);

        DividerItemDecoration horizontalDecoration = new DividerItemDecoration(TableOfContentsActivity.this,
                DividerItemDecoration.HORIZONTAL);
        Drawable horizontalDivider = ContextCompat.getDrawable(TableOfContentsActivity.this, R.drawable.horizontal_divider);
        assert horizontalDivider != null;
        horizontalDecoration.setDrawable(horizontalDivider);
        recyclerView.addItemDecoration(horizontalDecoration);

        //get the table of contents data with updated data
        // from the database
        getTableOfContentsData(userId, currentNotebookID);

        //first create a new note page/entry in firebase db with ID
        // via a dialog
        createNewEntryBtn.setOnClickListener(view -> {
            if (sharedNotebookPermission != null) {
                // check for permissions level ---viewer or editor
                //good
                if (sharedNotebookPermission.matches("Viewer")) {
                    // shared notebook viewer level access
                    Toast.makeText(TableOfContentsActivity.this, "Need Editor Access", Toast.LENGTH_SHORT).show();

                } else {
                    // shared notebook Editor level access
                    // allow creation
                    showCreateNoteDialog();

                }
            } else {
                // default behavior for actual user
                // show dialog first
                // check notebook lock status
                if (isNotebookLocked.matches("No")) {
                    // notebook is not locked
                    // can create entry
                    showCreateNoteDialog();
                } else {
                    //notebook is locked
                    //can't add to it
                    Toast.makeText(TableOfContentsActivity.this, "This notebook is Locked.", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    //handle click on back button
    //on click send user to main activity
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
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
                            Log.w(TAG, "NoteBk isLock = " + isNotebookLocked);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "NoteBk isLock error: " + e);
                        }
                    });
        }
    }


    // display the custom dialog to create a new entry
    private void showCreateNoteDialog() {
        final Dialog dialog = new Dialog(TableOfContentsActivity.this);
        // disable default title
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        // only allow cancel with button
        dialog.setCancelable(false);
        // inflate with created layout
        dialog.setContentView(R.layout.new_note_page_dialog);
        // initialize the views
        final EditText subjectEt = dialog.findViewById(R.id.subject_et);
        Button cancelBtn = dialog.findViewById(R.id.cancelNote_btn);
        Button createBtn = dialog.findViewById(R.id.createNote_btn);

        // create new entry in the database
        createBtn.setOnClickListener(view -> {
            final String noteTitle = subjectEt.getText().toString();

            // structure in database
            // pages > userID >> notes >> notebookID >> entries >> note_id with its info (date created, id, subject, time created)
            // Create a new note document in Firestore with the provided title, date, and time
            final String currentDate = getCurrentDate();
            final String currentTime = getCurrentTime();

            //create new Note object with user provided title
            //note creation date (currentDate), and creation time (currentTime)
            // last modified date initially same as created
            //assign to newNote
            //String title = "Learn to play";
            //Notebook newNote = new Notebook(title, currentDate, currentTime);
            InitialNote newNote = new InitialNote(noteTitle, currentDate, currentTime, currentDate);
            try {
                //String userId1 = user.getUid();

                // Structure
                // pages > userID >> notes >> notebookID >> entries >> created note_id
                fStore.collection("pages").document(userId)
                        .collection("notes").document(currentNotebookID)
                        .collection("entries").add(newNote)
                        .addOnSuccessListener(documentReference -> {
                            // Get the auto-generated ID of the newly created note document
                            final String noteId = documentReference.getId();

                            // Create a HashMap to store the "id" fields of the note
                            Map<String, Object> noteData = new HashMap<>();
                            noteData.put("noteID", noteId);

                            // Update the note document with the "id"
                            documentReference.update(noteData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.w(TAG, "Successfully created new entry with ID: " + noteId);
                                        // update notebook date stamp
                                        updateNotebookStamp(currentNotebookID);

                                        //update the table of contents in real time
                                        // add this note entry to current list
                                        initialNoteList.add(new InitialNote(noteId, noteTitle, currentDate, currentTime, currentDate));

                                        // update the recycler adapter
                                        entryRecyclerAdapter.setItemsList(initialNoteList);
                                        entryRecyclerAdapter.notifyDataSetChanged();
                                    })
                                    .addOnFailureListener(e -> {

                                        // Handle the case where updating the notebook document failed
                                        Toast.makeText(TableOfContentsActivity.this, "Error creating note page ", Toast.LENGTH_SHORT).show();
                                    });

                        })
                        .addOnFailureListener(e -> {
                            // Handle the case where creating the notebook document failed
                            Log.w(TAG, "Failed to create new entry" + e.getMessage());
                            Toast.makeText(TableOfContentsActivity.this, "Error creating note page: ", Toast.LENGTH_SHORT).show();
                        });
            } catch (Exception e) {
                Log.w(TAG, e.toString());
            }
            dialog.dismiss();
        });

        // close the dialog on cancel
        cancelBtn.setOnClickListener(view -> dialog.dismiss());

        dialog.show();
    }

    private String getCurrentDate() {
        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);

        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()); //dd/MM/yyyy

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

    private void getTableOfContentsData(String userId, String currentNotebookID) {
        // structure in database
        // pages > userID >> notes >> notebookID >> entries >> note_id with its info (date created, id, subject, time created)
        String path = "pages/" + userId + "/notes/" + currentNotebookID + "/entries";
        final CollectionReference collectionReference = fStore.collection(path);
        try {
            collectionReference
                    .orderBy("dateCreated", Query.Direction.ASCENDING)
                    .get().addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                        @Override
                        public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                            if (queryDocumentSnapshots != null) {
                                //initialNoteList = new ArrayList<>();

                                for (DocumentSnapshot documentSnapshot : queryDocumentSnapshots) {
                                    Log.d(TAG, "success of getting notes" + documentSnapshot);
                                    Log.d(TAG, "success of getting notesID: " + documentSnapshot.getString("noteID"));

                                    String title = documentSnapshot.getString("title");
                            String noteID = documentSnapshot.getString("noteID");
                            String dateCreated = documentSnapshot.getString("dateCreated");
                            String timeCreated = documentSnapshot.getString("timeCreated");
                            String lastModifiedDate = documentSnapshot.getString("lastModifiedDate");

                            if (noteID != null) {
                                // make sure note id is not null
                                initialNoteList.add(new InitialNote(noteID, title, dateCreated, timeCreated, lastModifiedDate));
                            }

                        }
                        entryRecyclerAdapter.setItemsList(initialNoteList);
                        entryRecyclerAdapter.notifyDataSetChanged();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, e.toString());
                }
            });

        } catch (Exception e) {
            Log.w(TAG, e.toString());
        }
    }
}




