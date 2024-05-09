package com.example.engineeringnotebook.Note;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.BackgroundColorSpan;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.engineeringnotebook.DigitalInk.DigitalInkMainActivity;
import com.example.engineeringnotebook.Model.Note;
import com.example.engineeringnotebook.R;
import com.example.engineeringnotebook.utils.LogHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EditNote extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(EditNote.class);
    private Intent data;
    private EditText editNoteTitle, editNoteContent;
    private FloatingActionButton fab;
    private FirebaseAuth mAuth = null;
    private FirebaseFirestore fStore = null;
    private FirebaseDatabase mDatabase = null;
    private ProgressBar progressBarEditNote;
    private FirebaseUser user = null;
    private String notebookId, notebookName, noteId, noteTitle;
    private String userId = null;
    private String notebookOwnerID = null;
    private boolean isExpanded = false;
    private boolean isMainButtonVisible = false;
    FirebaseStorage storage1;
    private Button penToolBtn, highlightToolBtn;

    public enum Tool {
        TEXT, //enum value representing the Text tool
        PEN, //enum value representing the Pen tool
        LASSO, //enum value representing the Lasso tool
        HIGHLIGHTER //enum value representing the Highlighter tool
    }

    private Tool selectedTool = Tool.TEXT; //initialize selected tool with default value

    private Button attachment_btn;
    private ImageView imageView,imgview;

    //Uri indicates, where the image will be picked from
     private Uri image,image2 ;


    private StorageReference reference;
    private WebView webView;


    //instance for firebase storage and reference
    private FirebaseStorage storage;
    StorageReference storageReference;
    private String noteContent = null, sharedNotebookPermission = null;
    private String lockStatus = null;

//Launch camera
    ActivityResultLauncher<Intent> takePicture = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult o) {
            if (o.getResultCode() == RESULT_OK && o.getData() != null) {
                Bundle bundle = o.getData().getExtras();
                    Bitmap bitmap = (Bitmap) bundle.get("data");
                //Get the file path to the downloads folder
                String downloadsDirectoryPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath();
                File downloadsDirectory = new File(downloadsDirectoryPath);


                //Get the date and time and set it as filename
                    Date date = new Date();
                    SimpleDateFormat format = new SimpleDateFormat("yyyy_MM_dd_hh_mm_ss");
                    String timeStamp = format.format(date);
                    String Filename = timeStamp;


                    //Save the image into the downloads folder
                    try {
                        File filePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),Filename+ ".png");
                        FileOutputStream outputStream = new FileOutputStream(filePath);

                        bitmap.compress(Bitmap.CompressFormat.PNG,100,outputStream);
                        outputStream.flush();
                        outputStream.close();

                        Toast.makeText(EditNote.this, Filename + "Successfully saved in downloads", Toast.LENGTH_SHORT).show();
                    } catch (Exception e){
                        e.printStackTrace();
                    }

                    //Refresh Galery after a picture has been taken
                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                    Uri contentUri = Uri.parse("file://" + downloadsDirectoryPath);
                    mediaScanIntent.setData(contentUri);
                    Context context = EditNote.this;
                    context.sendBroadcast(mediaScanIntent);


                    //Convert bitmap to uri
                    File file = new File(getCacheDir(), Filename);

                   //sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

                    try(FileOutputStream out = new FileOutputStream(file)) {
                        bitmap.compress(Bitmap.CompressFormat.PNG,100,out);
                        out.close();
                    } catch (IOException e){
                        e.printStackTrace();
                    }

                    Uri imageUri = Uri.fromFile(file);


                    String result = entrylocation();

                //Get image Name
                File imgName = new File(imageUri.getPath());
                String imageName = imgName.getName();

                //upload the Image
                StorageReference reference2;
                reference2 = uploadImage(imageUri);

                editNoteContent.append(" " + Filename + ".jpg" + "   " );

                ClickableImage();

                //Creating clickable span to be  implemented as a link
                ClickableSpan clickableSpan2 = new ClickableSpan(){
                    @Override
                    public void onClick(  View widget) {


                        ClickableImage();
                        dialogwebview(result,Filename);


                    }
                };

                SpannableString spannableString2 = new SpannableString(imageName + ".jpg ");
                String text_location = editNoteContent.getText().toString();

                spannableString2.setSpan(clickableSpan2,0,23,Spanned.SPAN_INCLUSIVE_INCLUSIVE);
                //editNoteContent.append(spannableString2);
                editNoteContent.setMovementMethod(LinkMovementMethod.getInstance());

            }

        }
    });




    private final ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
        @Override
        public void onActivityResult(ActivityResult result) {
            if (result.getResultCode() == RESULT_OK) {
                if (result.getData() != null) {

                    image = result.getData().getData();

                    //Get image Name
                    File imgName = new File(image.getPath());
                    String imageName = imgName.getName();

                    String location = entrylocation();

                    //upload the Image
                    StorageReference reference1;
                    reference1 = uploadImage(image);

                    //Get the location of the Image
                    StorageReference storageReference1 = storage1.getReference().child( reference1.getPath());

                    //Format file path for web View
                    String savedimage = storageReference1.getPath().toString().replaceFirst("/","").replace("/", "%2F");

                    File file = new File(savedimage);

                    //Creating clickable span to be  implemented as a link
                    ClickableSpan clickableSpan = new ClickableSpan(){
                        @Override
                        public void onClick(  View widget) {
                            dialogwebview(location,imageName);
                      }
                    };

                    SpannableString spannableString1 = new SpannableString(imageName + ".jpg ");

                    String text_location = editNoteContent.getEditableText().toString();


                    int start = text_location.indexOf(imageName);
                    int end = start + imageName.length();

                    spannableString1.setSpan(clickableSpan,0,end+5,Spanned.SPAN_INCLUSIVE_INCLUSIVE);

                    editNoteContent.append("   " + spannableString1);
                    editNoteContent.setMovementMethod(LinkMovementMethod.getInstance());
                    //Make the image name clickable
                    ClickableImage();

                }
            } else {
                Toast.makeText(EditNote.this, "Please select an image", Toast.LENGTH_SHORT).show();
            }
        }
    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_note);
        Toolbar toolbar = findViewById(R.id.toolbar);
        editNoteContent = findViewById(R.id.editNoteContent);
        editNoteTitle = findViewById(R.id.editNoteTitle);
        progressBarEditNote = findViewById(R.id.progressBar2);
        //initialise views
        attachment_btn = findViewById(R.id.attachment_btn);
        imageView = findViewById(R.id.imgView);
        imageView = findViewById(R.id.imgView);
        imgview =  findViewById(R.id.imgView);

        //Initializing firebase storage
        storage1 = FirebaseStorage.getInstance();
        FirebaseApp.initializeApp(getApplicationContext());

        webView = findViewById(R.id.webview);
        fab = findViewById(R.id.saveEditedNote);
        setSupportActionBar(toolbar);

        penToolBtn = findViewById(R.id.penToolBtn);
        highlightToolBtn = findViewById(R.id.highlighterToolBtn);

        // init firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        user = mAuth.getCurrentUser();
        fStore = FirebaseFirestore.getInstance();

        // Get notebook ID and notebook name from the intent extras
        data = getIntent();
        //extract data
        //String noteTitle = data.getStringExtra("title");
        if (data != null) {
            notebookId = data.getStringExtra("notebookID");
            notebookName = data.getStringExtra("notebookName");
            noteId = data.getStringExtra("noteId");
            noteTitle = data.getStringExtra("noteTitle");
            noteContent = data.getStringExtra("content");
            notebookOwnerID = data.getStringExtra("shareNoteOwnerID");
            sharedNotebookPermission = data.getStringExtra("permissions");

            if (notebookOwnerID != null) {
                // set userID to ownerID for shared notebooks to get notebook data
                Log.w(TAG, "Shared NoteBk owner ID = " + notebookOwnerID);
                userId = notebookOwnerID;

                // check for permissions level ---viewer or editor
                if (sharedNotebookPermission != null) {
                    //good
                    if (sharedNotebookPermission.matches("Viewer")) {
                        // shared notebook viewer level access
                        editNoteTitle.setEnabled(false);
                        editNoteContent.setEnabled(false);
                        editNoteContent.setTextColor(Color.BLACK);

                    } else {
                        // shared notebook Editor level access
                        editNoteTitle.setEnabled(true);
                        editNoteContent.setEnabled(true);
                    }
                }
            } else {
                // default user
                Log.w(TAG, "Error intent, no shared NoteBook data.");
                userId = user.getUid();
            }
        } else {
            // default user
            userId = user.getUid();
            Log.w(TAG, "Error intent, no NoteBook data. ");
        }

        // get this notebook's current locked status from db
        getNotebookLockStatus(userId, notebookId);

        //set noteTitle to editNoteTitle for edit and delete of currently present note
        editNoteTitle.setText(noteTitle);
        editNoteContent.setText(noteContent);
        // retrieve current note contents
        getCurrentNoteText();

        //Get the Firebase storage Reference
        storageReference = FirebaseStorage.getInstance().getReference();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //display progress bar after save note is clicked
                progressBarEditNote.setVisibility(View.VISIBLE);
                if (sharedNotebookPermission != null) {
                    // check for permissions level ---viewer or editor
                    //good
                    if (sharedNotebookPermission.matches("Viewer")) {
                        // shared notebook viewer level access
                        Toast.makeText(EditNote.this, "Need Editor Access", Toast.LENGTH_SHORT).show();

                    } else {
                        // shared notebook Editor level access
                        // allow access
                        String nTitle = editNoteTitle.getText().toString();
                        String nContent = editNoteContent.getText().toString();

                        //check if empty, throw error message if empty
                        if (nTitle.isEmpty() || nContent.isEmpty()) {
                            Toast.makeText(EditNote.this, "Can not save not with Empty Field", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // call function to save this note
                        saveNoteToRealtimeDB(nTitle, nContent);
                    }
                } else {
                    // default behavior for current user
                    // check lock status
                    if (lockStatus.matches("No")) {
                        //insert edited note into database if title and content of edit note isn't empty
                        //extract string passed through noteContent and noteTitle
                        String nTitle = editNoteTitle.getText().toString();
                        String nContent = editNoteContent.getText().toString();

                        //check if empty, throw error message if empty
                        if (nTitle.isEmpty() || nContent.isEmpty()) {
                            Toast.makeText(EditNote.this, "Can not save not with Empty Field", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        //display progress bar after save note is clicked
                        //progressBarEditNote.setVisibility(View.VISIBLE);

                        // call function to save this note
                        saveNoteToRealtimeDB(nTitle, nContent);

                    } else {
                        // notebook is locked
                        //can't add to it
                        Toast.makeText(EditNote.this, "This notebook is Locked.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // on pressing the attachment button SelectImage() is called
        attachment_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sharedNotebookPermission != null) {
                    // check for permissions level ---viewer or editor
                    //good
                    if (sharedNotebookPermission.matches("Viewer")) {
                        // shared notebook viewer level access
                        Toast.makeText(EditNote.this, "Need Editor Access", Toast.LENGTH_SHORT).show();

                    } else {
                        // shared notebook Editor level access
                        // allow access
                        PopupMenu popupMenu = new PopupMenu(EditNote.this, attachment_btn);

                        popupMenu.getMenuInflater().inflate(R.menu.popupmenu, popupMenu.getMenu());
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                if (menuItem.getItemId() == R.id.id1) {
                                    String path = Environment.getExternalStorageDirectory() + "/" + "Downloads" + "/";
                                    Uri uri = Uri.parse(path);
                                    Intent intent = new Intent(Intent.ACTION_PICK);
                                    //intent.setType("image/*");
                                    intent.setDataAndType(uri,"*/*");
                                    activityResultLauncher.launch(intent);

                                } else if (menuItem.getItemId() == R.id.id4) {
                                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    takePicture.launch(intent);



                                } else {
                                    return false;
                                }

                                return false;
                            }
                        });

                        popupMenu.show();
                    }
                } else {
                    // default behavior for current user
                    // check lock status
                    if (lockStatus.matches("No")) {
                        PopupMenu popupMenu = new PopupMenu(EditNote.this, attachment_btn);

                        popupMenu.getMenuInflater().inflate(R.menu.popupmenu, popupMenu.getMenu());
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                if (menuItem.getItemId() == R.id.id1) {
                                    Intent intent = new Intent(Intent.ACTION_PICK);
                                    intent.setType("image/*");
                                    activityResultLauncher.launch(intent);
                                } else if (menuItem.getItemId() == R.id.id4) {
                                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                    takePicture.launch(intent);


                                } else {
                                    return false;
                                }

                                return false;
                            }
                        });

                        popupMenu.show();
                    } else {
                        // notebook is locked
                        //can't add to it
                        Toast.makeText(EditNote.this, "This notebook is Locked.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // for handwriting tool
        penToolBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sharedNotebookPermission != null) {
                    // check for permissions level ---viewer or editor
                    //good
                    if (sharedNotebookPermission.matches("Viewer")) {
                        // shared notebook viewer level access
                        Toast.makeText(EditNote.this, "Need Editor Access", Toast.LENGTH_SHORT).show();

                    } else {
                        // shared notebook Editor level access
                        // allow
                        selectedTool = Tool.PEN; //set selected tool
                        //updateMainToolsButtonBackground(); //update background of mainToolsBtn

                        // go to canvas for ink writing
                        Intent intent = new Intent(getApplicationContext(), DigitalInkMainActivity.class);
                        intent.putExtra("notebookID", notebookId);
                        intent.putExtra("noteId", noteId);
                        intent.putExtra("noteTitle", editNoteTitle.getText().toString());
                        intent.putExtra("contentNow", editNoteContent.getText().toString());
                        intent.putExtra("shareNoteOwnerID", notebookOwnerID);
                        intent.putExtra("permissions", sharedNotebookPermission);
                        startActivity(intent);

                    }
                } else {
                    // default behavior for current user
                    // check lock status
                    if (lockStatus.matches("No")) {
                        selectedTool = Tool.PEN; //set selected tool
                        //updateMainToolsButtonBackground(); //update background of mainToolsBtn

                        // go to canvas for ink writing
                        Intent intent = new Intent(getApplicationContext(), DigitalInkMainActivity.class);
                        intent.putExtra("notebookID", notebookId);
                        intent.putExtra("noteId", noteId);
                        intent.putExtra("noteTitle", editNoteTitle.getText().toString());
                        intent.putExtra("contentNow", editNoteContent.getText().toString());
                        intent.putExtra("shareNoteOwnerID", notebookOwnerID);
                        intent.putExtra("permissions", sharedNotebookPermission);
                        startActivity(intent);
                    } else {
                        // notebook is locked
                        //can't add to it - only view
                        //Toast.makeText(EditNote.this, "This notebook is Locked.", Toast.LENGTH_SHORT).show();
                        // go to canvas for ink writing
                        Intent intent = new Intent(getApplicationContext(), DigitalInkMainActivity.class);
                        intent.putExtra("notebookID", notebookId);
                        intent.putExtra("noteId", noteId);
                        intent.putExtra("noteTitle", editNoteTitle.getText().toString());
                        intent.putExtra("contentNow", editNoteContent.getText().toString());
                        intent.putExtra("shareNoteOwnerID", notebookOwnerID);
                        intent.putExtra("permissions", sharedNotebookPermission);
                        startActivity(intent);
                    }
                }
            }
        });


        highlightToolBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (sharedNotebookPermission != null) {
                    // check for permissions level ---viewer or editor
                    //good
                    if (sharedNotebookPermission.matches("Viewer")) {
                        // shared notebook viewer level access
                        Toast.makeText(EditNote.this, "Need Editor Access", Toast.LENGTH_SHORT).show();

                    } else {
                        // shared notebook Editor level access
                        // allow
                        if (selectedTool == Tool.HIGHLIGHTER) {
                            // Get the selected text range
                            int startSelection = editNoteContent.getSelectionStart();
                            int endSelection = editNoteContent.getSelectionEnd();

                            // Check if text is selected
                            if (startSelection >= 0 && endSelection >= 0) {
                                // Toggle text highlighting for the selected range
                                toggleTextHighlight(startSelection, endSelection);
                            } else {
                                Toast.makeText(EditNote.this, "Select text to highlight first", Toast.LENGTH_SHORT).show();
                            }
                        }

                    }
                } else {
                    // default behavior for current user
                    // check lock status
                    if (lockStatus.matches("No")) {
                        selectedTool = Tool.HIGHLIGHTER; //set selected tool
                        //updateMainToolsButtonBackground(); //update background of mainToolsBtn
                        if (selectedTool == Tool.HIGHLIGHTER) {
                            // Get the selected text range
                            int startSelection = editNoteContent.getSelectionStart();
                            int endSelection = editNoteContent.getSelectionEnd();

                            // Check if text is selected
                            if (startSelection >= 0 && endSelection >= 0) {
                                // Toggle text highlighting for the selected range
                                toggleTextHighlight(startSelection, endSelection);
                            } else {
                                Toast.makeText(EditNote.this, "Select text to highlight first", Toast.LENGTH_SHORT).show();
                            }
                        }

                    } else {
                        // notebook is locked
                        //can't add to it
                        Toast.makeText(EditNote.this, "This notebook is Locked.", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });
    }

    private void getCurrentNoteText() {
        // retrieve note content from realtime db
        try {
            String notebookID = "/" + notebookId + "/";

            //Note note = new Note("UTA 2023", "By default, read and write access to your database is restricted so only authenticated users can read or write data. To get started without setting up Authentication, you can configure your rules for public access. This does make your database open to anyone, even people not using your app, so be sure to restrict your database again when you set up authentication.");
            //Note note = new Note(title, content);

            // Structure
            // entries > userID >> notebookID >> note entry/page_ID >> content to save
            DatabaseReference ref = mDatabase.getReference("entries/" + userId + notebookID + noteId);

            // Attach a listener to read the data at our posts reference
            ref.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    Note note = dataSnapshot.getValue(Note.class);

                    //set editText to text
                    if (note != null) {
                        editNoteContent.setText(note.getContent());
                        //testing getting input
                        ClickableImage();
                    } else {
                        // nothing in note contents
                        editNoteContent.setText("");
                    }

                    Log.w(TAG, "Note fetched.." + note);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    Log.w(TAG, "Note fetch error.." + databaseError.getCode());
                }
            });

        } catch (Exception e) {
            Log.w(TAG, e.toString());
            progressBarEditNote.setVisibility(View.INVISIBLE);
        }

    }

    // Method to toggle text highlighting
    //working
    private void toggleTextHighlight(int start, int end) {
        if (selectedTool == Tool.HIGHLIGHTER) {
            // Get the selected text range
            int min = Math.max(0, start);
            int max = Math.min(editNoteContent.length(), end);

            //ToDo make color dynamic
            //use GREEEN YELLOW CYAN RED

            //default to yellow
            String color="YELLOW";
            //change color if user clicks dif color
            //if( user click <color>) { color=<color> }

            if (min < max) {
                SpannableStringBuilder builder = new SpannableStringBuilder(editNoteContent.getText());
                BackgroundColorSpan[] existingSpans = builder.getSpans(min, max, BackgroundColorSpan.class);
                //check if already highlighted
                if (existingSpans.length > 0) {
                    for (BackgroundColorSpan span : existingSpans) {
                        int spanStart = builder.getSpanStart(span);
                        int spanEnd = builder.getSpanEnd(span);
                        //if already highlighted and clicked button then remove highloght
                        if (spanStart >= min && spanEnd <= max) {
                            builder.removeSpan(span);
                        } else {
                            if (spanStart < min && spanEnd > max) {
                                builder.removeSpan(span);
                                builder.setSpan(new BackgroundColorSpan(Color.parseColor(color)), spanStart, min, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                builder.setSpan(new BackgroundColorSpan(Color.parseColor(color)), max, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else if (spanStart < min) {
                                builder.removeSpan(span);
                                builder.setSpan(new BackgroundColorSpan(Color.parseColor(color)), spanStart, min, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            } else if (spanEnd > max) {
                                // Overlaps the end of the selected range
                                builder.removeSpan(span);
                                builder.setSpan(new BackgroundColorSpan(Color.parseColor(color)), max, spanEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                            }
                        }
                    }
                } else {
                    BackgroundColorSpan highlightSpan = new BackgroundColorSpan(Color.parseColor(color));
                    builder.setSpan(highlightSpan, min, max, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }

                editNoteContent.setText(builder);
            } else {
                Toast.makeText(EditNote.this, "Select text to highlight first", Toast.LENGTH_SHORT).show();
            }
        }
    }


    // save this entry/note to Firebase realtime DB
    // params: String title --> new note title
    // params: String Content --> new note content
    private void saveNoteToRealtimeDB(String title, String content) {
        // save note to realtime db
        try {
            //String userId = user.getUid();
            String notebookID = "/" + notebookId + "/";

            Note note = new Note(title, content);
            // Structure
            // entries > userID >> notebookID >> note entry/page_ID >> content to save
            mDatabase.getReference("entries/" + userId +
                    notebookID + noteId).setValue(note).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.w(TAG, "Successfully saved note info..");
                    //Toast.makeText(EditNote.this, "Note saved successfully!", Toast.LENGTH_SHORT).show();

                    // update notebook date stamp
                    updateNotebookStamp(notebookId);

                    // also update the note title in firestore db
                    // also update lastModified date of this note
                    // pages > userID >> notes >> notebookID >> entries >> created note_id
                    final String currentDate = getCurrentDate();
                    Map<String, Object> noteData = new HashMap<>();
                    noteData.put("title", title);
                    noteData.put("lastModifiedDate", currentDate);

                    fStore.collection("pages").document(userId)
                            .collection("notes").document(notebookId)
                            .collection("entries").document(noteId).update("title", title, "lastModifiedDate", currentDate)
                            .addOnSuccessListener(e -> Log.w(TAG, "Successfully updated note title and date in firestore db"));
                    progressBarEditNote.setVisibility(View.INVISIBLE);
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Failure to save note info.." + e.toString());
                    Toast.makeText(EditNote.this, "Failed to save Note", Toast.LENGTH_SHORT).show();
                    progressBarEditNote.setVisibility(View.INVISIBLE);
                }
            });

        } catch (Exception e) {
            Log.w(TAG, e.toString());
            progressBarEditNote.setVisibility(View.INVISIBLE);
        }
        ClickableImage();
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
                                if (documentSnapshot.getString("isLocked").matches("Yes")) {
                                    // disable editText if notebook is locked
                                    // notebook is locked
                                    editNoteTitle.setEnabled(false);
                                    editNoteContent.setEnabled(false);
                                    editNoteContent.setTextColor(Color.BLACK);
                                }
                                lockStatus = documentSnapshot.getString("isLocked");
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

    //Display X on toolbar to exit activity without saving note
    //override onCreateOptionsMenu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.close_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    //Handle click of 'X'
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.closeBtn)
        {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    private StorageReference uploadImage(Uri image) {

        String notebookID = "/" + notebookId + "/";
        String entryID = "/" + noteId + "/";

        //Get the file path of the image
        try {
            String path = image.getPath(); // here
            File name = new File(path);
            ///get the file name of the image
            String Filename = name.getName();


            //Sets the location and name of location of where you want to store the files
            //String.valueOf(user) can be changed to path you want to store it in
            //StorageReference reference = storageReference.child(String.valueOf(user) + Filename);
            reference = storageReference.child("Images/" + userId +
                    notebookID + entryID + Filename);
            reference.putFile(image).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    Toast.makeText(EditNote.this, "File upload successful", Toast.LENGTH_SHORT).show();
                    //editNoteContent.setText(reference.toString());

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Toast.makeText(EditNote.this, "There was an error while uploading file", Toast.LENGTH_SHORT).show();
                }
            });
        }
        catch (NullPointerException ex) {
            Log.w(TAG, "Error. Couldn't not save the image because image path is null" + ex.toString());
        }

        return reference;

    }


    public  String entrylocation(){

        String notebookID = "/" + notebookId + "/";
        String entryID = "/" + noteId + "/";

        //location of the current entry in the database
        reference = storageReference.child("Images/" + userId + notebookID + entryID);

        //Delete everything before Images
        String needschange = reference.toString();
        String specificword = "appspot.com";

        String pattern1 = ".*\\b" + specificword + "\\b";

        Pattern r = Pattern.compile(pattern1);
        Matcher matcher1 = r.matcher(needschange);

        //Replace all / with %2F
        String result = matcher1.replaceAll("").replaceFirst("/","").replace("/", "%2F");

        return result;
    }
    public void ClickableImage() {


        String result = entrylocation();

        String edittextvalue =  editNoteContent.getEditableText().toString();
        //Make every word that ends with jpg clickable in edit note
        Pattern pattern = Pattern.compile("\\b\\w+\\.jpg\\b");

        Matcher matcher = pattern.matcher(edittextvalue);

        List<String> jpgWords =  new ArrayList<>();

        while (matcher.find()){
            jpgWords.add(matcher.group());
        }

        for (String jpgWord: jpgWords){
           ClickableSpan clickableSpan = new ClickableSpan() {
               @Override
               public void onClick(@NonNull View widget) {

                   //StorageReference reference1 = storage1.getReference();
                   dialogwebview(result,jpgWord.replace(".jpg",""));


               }
           };

            SpannableString spannableString = new SpannableString(jpgWord);
            spannableString.setSpan(clickableSpan,0,spannableString.length(),Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            Editable editable = editNoteContent.getEditableText();
           int startindex = editable.toString().indexOf(jpgWord);
           int endindex = startindex + jpgWord.length();
           if(startindex >= 0){
               editable.delete(startindex,endindex);
               editable.insert(startindex,spannableString);
           }
           editNoteContent.setMovementMethod(LinkMovementMethod.getInstance());
        }

    }



    public void dialogwebview( String entryname, String imagename){

        //Creating a dialog box to hold the web view
        AlertDialog.Builder alert = new AlertDialog.Builder(EditNote.this);
        Editable editabledialogbox = editNoteContent.getEditableText();
        int startindex = editabledialogbox.toString().indexOf(imagename);
        int endindex = startindex + imagename.length() + 4;

        WebView wv = new WebView(EditNote.this);

        wv.getSettings().setLoadWithOverviewMode(true);
        wv.getSettings().setUseWideViewPort(true);
        String url = "https://firebasestorage.googleapis.com/v0/b/digital-engineering-note-43313.appspot.com/o/" +entryname+ "%2F" + imagename + "?alt=media";
        wv.loadUrl(url);

        wv.setWebViewClient(new WebViewClient(){
            @Override
            public boolean shouldOverrideUrlLoading(WebView view,String url){
                view.loadUrl(url);
                return true;
            }
        });

        alert.setView(wv);
        alert.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                editabledialogbox.delete(startindex,endindex);
                delfromdb(imagename);
            }
        });
        alert.setNegativeButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

            }
        });
        alert.show();


    }

    private String getCurrentDate() {
        Date c = Calendar.getInstance().getTime();
        System.out.println("Current time => " + c);

        SimpleDateFormat df = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());

        return df.format(c);
    }

    private String getCurrentTime() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR);
        int minute = calendar.get(Calendar.MINUTE);
        int amPm = calendar.get(Calendar.AM_PM);

        String amPmIndicator = (amPm == Calendar.AM) ? "AM" : "PM";

        return hour + ":" + minute + " " + amPmIndicator;
    }

    public void delfromdb(String imagename){

        String notebookID = "/" + notebookId + "/";
        String entryID = "/" + noteId + "/";

        String location = entrylocation();
        //location of the current entry in the database
        StorageReference imageRef;
        imageRef = storageReference.child("Images/" + userId + notebookID + entryID + imagename);
        imageRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // Image deleted successfully
                        Toast.makeText(EditNote.this, "Image deleted successfully", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle any errors
                        Toast.makeText(EditNote.this, "Error deleting image: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }
}



