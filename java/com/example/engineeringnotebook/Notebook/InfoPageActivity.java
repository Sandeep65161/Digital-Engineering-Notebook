package com.example.engineeringnotebook.Notebook;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.engineeringnotebook.Model.InformationPage;
import com.example.engineeringnotebook.R;
import com.example.engineeringnotebook.utils.LogHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class InfoPageActivity extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(InfoPageActivity.class);
    //variables needed
    private EditText editTextName;
    private EditText editTextNotebookNo;
    private ImageView showSignatureImageView;
    private EditText editTextDate;
    private EditText editTextDateIssued;
    private EditText editTextIssuedBy;
    private EditText editTextPhone;
    private EditText editTextEmail;
    private EditText editTextCompany;
    private EditText editTextDepartment;
    private EditText editTextAddress;
    private EditText editTextCity;
    private EditText editTextState;
    private EditText editTextZip;
    private EditText editTextDateCompleted;
    private EditText editTextPagesFilled;
    private EditText editTextContinuedFromNotebookNo;
    private EditText editTextContinuedToNotebookNo;
    private String infoID, notebookId;


    private FirebaseFirestore fStore;
    private FirebaseAuth fAuth;
    private InformationPage infoPageData;

    private FirebaseFirestore db;

    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;


    private Button editButton;
    private Button saveButton;
    private Map<String, Object> infoDataMap;
    private String userId = null, ownerID = null, sharedNotebookPermission = null;
    private String lockStatus = null;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.information_page);
        //find field/buttons from xml file by referencing ID
        editTextName = findViewById(R.id.editTextName);
        editTextNotebookNo = findViewById(R.id.editTextNotebookNo);
        showSignatureImageView = findViewById(R.id.showSign_imageView);
        editTextDate = findViewById(R.id.editTextDate);
        editTextDate.setEnabled(false);
        editTextDateIssued = findViewById(R.id.editTextDateIssued);
        editTextIssuedBy = findViewById(R.id.editTextIssuedBy);
        editTextPhone = findViewById(R.id.editTextPhone);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextCompany = findViewById(R.id.editTextCompany);
        editTextDepartment = findViewById(R.id.editTextDepartment);
        editTextAddress = findViewById(R.id.editTextAddress);
        editTextCity = findViewById(R.id.editTextCity);
        editTextState = findViewById(R.id.editTextState);
        editTextZip = findViewById(R.id.editTextZip);
        editTextDateCompleted = findViewById(R.id.editTextDateCompleted);
        editTextPagesFilled = findViewById(R.id.editTextPagesFilled);
        editTextContinuedFromNotebookNo = findViewById(R.id.editTextContinuedFromNotebookNo);
        editTextContinuedToNotebookNo = findViewById(R.id.editTextContinuedToNotebookNo);
        editButton = findViewById(R.id.editButton);
        saveButton = findViewById(R.id.saveButton);
        editTextNotebookNo.setEnabled(false);

        //Add notebook title to Table of content screen
        getSupportActionBar().setTitle("Information Page");

        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();
        infoPageData = new InformationPage();
        db = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        Intent intent = getIntent();

        if (intent != null) {
            // get notebook ids
            notebookId = intent.getStringExtra("notebookId");
            infoID = intent.getStringExtra("infoID");
            ownerID = intent.getStringExtra("ownerID");
            lockStatus = intent.getStringExtra("isLocked");
            sharedNotebookPermission = intent.getStringExtra("permissions");

            if (ownerID != null) {
                // set userID to ownerID for shared notebooks to get notebook data - shared notebook
                userId = ownerID;

                // check for permissions level ---viewer or editor
                if (sharedNotebookPermission != null) {
                    //good
                    if (sharedNotebookPermission.matches("Viewer")) {
                        // shared notebook viewer level access

                    } else {
                        // shared notebook Editor level access
                    }
                }
            } else {
                // default user-- current user stuff
                userId = fAuth.getCurrentUser().getUid();
            }

        } else {
            // no data from intent
            userId = fAuth.getCurrentUser().getUid();
        }

        // get notebook lock status
        getNotebookLockStatus(userId, notebookId);


        //get info from firebase
        if (notebookId == null) {
            Toast.makeText(InfoPageActivity.this, "Error getting the information.", Toast.LENGTH_SHORT).show();
        } else {
            // set signature imageView
            populateSignature();

            DocumentReference notebookRef = fStore.collection("notebookInfo").document(userId).collection(notebookId).document(infoID);

            notebookRef.get()
                    .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                        @Override
                        public void onSuccess(DocumentSnapshot documentSnapshot) {
                            if (documentSnapshot.exists()) {
                                InformationPage infoPage = documentSnapshot.toObject(InformationPage.class);
                                if (infoPage != null) {
                                    editTextName.setText(infoPage.getName());
                                    editTextNotebookNo.setText(infoPage.getNotebookNo());
                                    editTextDate.setText(infoPage.getDate());
                                    editTextDateIssued.setText(infoPage.getDateIssued());
                                    editTextIssuedBy.setText(infoPage.getIssuedBy());
                                    editTextPhone.setText(infoPage.getPhone());
                                    editTextEmail.setText(infoPage.getEmail());
                                    editTextCompany.setText(infoPage.getCompany());
                                    editTextDepartment.setText(infoPage.getDepartment());
                                    editTextAddress.setText(infoPage.getAddress());
                                    editTextCity.setText(infoPage.getCity());
                                    editTextState.setText(infoPage.getState());
                                    editTextZip.setText(infoPage.getZip());
                                    editTextDateCompleted.setText(infoPage.getDateCompleted());
                                }

                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(InfoPageActivity.this, "Failed to retrieve data", Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        //if click edit enable editing=true
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sharedNotebookPermission != null) {
                    // check for permissions level ---viewer or editor
                    //good
                    if (sharedNotebookPermission.matches("Viewer")) {
                        // shared notebook viewer level access
                        Toast.makeText(InfoPageActivity.this, "Need Editor Access", Toast.LENGTH_SHORT).show();
                        enableEditing(false);

                    } else {
                        // shared notebook Editor level access
                        // allow edit
                        enableEditing(true);
                    }
                } else {
                    // default behavior for current user
                    // check notebook lock status
                    if (lockStatus.matches("No")) {
                        // notebook is not locked
                        //enable edit is set true
                        enableEditing(true);
                    } else {
                        // notebook is locked
                        //so disable edit is set false
                        enableEditing(false);
                        Toast.makeText(InfoPageActivity.this, "Notebook is Locked.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        //if clicked "save" button enable edit = false edits the collection in firebase
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sharedNotebookPermission != null) {
                    // check for permissions level ---viewer or editor
                    //good
                    if (sharedNotebookPermission.matches("Viewer")) {
                        // shared notebook viewer level access
                        Toast.makeText(InfoPageActivity.this, "Need Editor Access", Toast.LENGTH_SHORT).show();
                        enableEditing(false);

                    } else {
                        // shared notebook Editor level access
                        // enable save functionality here
                        // save
                        doSave();
                    }
                } else {
                    // default behavior for current user
                    // check notebook lock status
                    if (lockStatus.matches("No")) {
                        // save
                        doSave();
                    } else {
                        // notebook is locked
                        Toast.makeText(InfoPageActivity.this, "Notebook is Locked.", Toast.LENGTH_SHORT).show();
                        enableEditing(false);
                    }
                }
            }
        });

    }

    private void doSave() {
        enableEditing(false);
        String name = editTextName.getText().toString();
        String notebookNo = editTextNotebookNo.getText().toString();
        String signature = "";
        String date = editTextDate.getText().toString();
        String dateIssued = editTextDateIssued.getText().toString();
        String issuedBy = editTextIssuedBy.getText().toString();
        String phone = editTextPhone.getText().toString();
        String email = editTextEmail.getText().toString();
        String company = editTextCompany.getText().toString();
        String department = editTextDepartment.getText().toString();
        String address = editTextAddress.getText().toString();
        String city = editTextCity.getText().toString();
        String state = editTextState.getText().toString();
        String zip = editTextZip.getText().toString();
        String dateCompleted = editTextDateCompleted.getText().toString();
        String pageFilled = editTextPagesFilled.getText().toString();
        String continuedFromNotebookNo = editTextContinuedFromNotebookNo.getText().toString();
        String continuedToNotebookNo = editTextContinuedToNotebookNo.getText().toString();

        InformationPage informationPage = new InformationPage(notebookNo, name, signature, date, issuedBy, dateIssued, phone, email, company, department, address, city, state, zip, dateCompleted, pageFilled, continuedFromNotebookNo, continuedToNotebookNo);

        Map<String, Object> updatedData = new HashMap<>();
        updatedData.put("name", name);
        updatedData.put("notebookNo", notebookNo);
        updatedData.put("signature", signature);
        updatedData.put("date", date);
        updatedData.put("dateIssued", dateIssued);
        updatedData.put("issuedBy", issuedBy);
        updatedData.put("phone", phone);
        updatedData.put("email", email);
        updatedData.put("company", company);
        updatedData.put("department", department);
        updatedData.put("address", address);
        updatedData.put("city", city);
        updatedData.put("state", state);
        updatedData.put("zip", zip);
        updatedData.put("dateCompleted", dateCompleted);
        updatedData.put("pageFilled", pageFilled);
        updatedData.put("continuedFromNotebookNo", continuedFromNotebookNo);
        updatedData.put("continuedToNotebookNo", continuedToNotebookNo);


        // Update the existing document in Firestore with the updated data
        fStore.collection("notebookInfo").document(userId).collection(notebookId).document(infoID)
                .update(updatedData)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(InfoPageActivity.this, "Update Successful", Toast.LENGTH_SHORT).show();

                        // also update the note date modified in firestore db
                        updateNotebookStamp(notebookId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(InfoPageActivity.this, "Update Failed" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

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

    private void populateSignature() {
        StorageReference signatureRef = storageReference.child("signatures/" +
                userId + "/" + notebookId + "/sign.png" );
        signatureRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @Override
            public void onSuccess(Uri uri) {
                Log.w(TAG, "Success in getting signature. ");
                Glide.with(InfoPageActivity.this).load(uri).into(showSignatureImageView);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Error getting signature." + e);
                //set placeholder image
                //Glide.with(InfoPageActivity.this).load("https://goo.gl/gEgYUd").into(showSignatureImageView);
            }
        });
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
                                lockStatus = documentSnapshot.getString("isLocked");
                                if (documentSnapshot.getString("isLocked").matches("Yes")) {
                                    // disable editTexts if notebook is locked
                                    // notebook is locked
                                    enableEditing(false);
                                }
                            } else {
                                // older data
                                // so set to No
                                lockStatus = "No";
                            }
                            Log.w(TAG, "NoteBk isLock = " + lockStatus);
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.d(TAG, "NoteBk isLock error: " + e);
                        }
                    });
        }
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


    private void enableEditing(boolean enable) {
        editTextName.setEnabled(enable);
        editTextNotebookNo.setEnabled(enable);
        editTextDate.setEnabled(false);
        editTextDateIssued.setEnabled(enable);
        editTextIssuedBy.setEnabled(enable);
        editTextPhone.setEnabled(enable);
        editTextEmail.setEnabled(enable);
        editTextCompany.setEnabled(enable);
        editTextDepartment.setEnabled(enable);
        editTextAddress.setEnabled(enable);
        editTextCity.setEnabled(enable);
        editTextState.setEnabled(enable);
        editTextZip.setEnabled(enable);
        editTextDateCompleted.setEnabled(enable);
        editTextContinuedFromNotebookNo.setEnabled(enable);
        editTextContinuedToNotebookNo.setEnabled(enable);


        editButton.setEnabled(!enable);
        saveButton.setEnabled(enable);
    }

    //Need to show X on toolbar to cancel Add Note Activity without saving note
    //override onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.close_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    //Handle click of 'X' - display message when clicked
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.closeBtn) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

}





