package com.example.engineeringnotebook.DigitalInk;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.engineeringnotebook.Model.Note;
import com.example.engineeringnotebook.Note.EditNote;
import com.example.engineeringnotebook.Notebook.InfoPageActivity;
import com.example.engineeringnotebook.R;
import com.example.engineeringnotebook.utils.LogHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.mlkit.vision.digitalink.DigitalInkRecognitionModelIdentifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Main activity which creates a StrokeManager and connects it to the DrawingView.
 */
public class DigitalInkMainActivity extends AppCompatActivity
        implements StrokeManager.DownloadedModelsChangedListener {
    private static final String TAG = LogHelper.makeLogTag(DigitalInkMainActivity.class);
    private static final String GESTURE_EXTENSION = "-x-gesture";
    private static final ImmutableMap<String, String> NON_TEXT_MODELS =
            ImmutableMap.of(
                    "zxx-Zsym-x-autodraw",
                    "Autodraw",
                    "zxx-Zsye-x-emoji",
                    "Emoji",
                    "zxx-Zsym-x-shapes",
                    "Shapes");

    private FirebaseAuth mAuth = null;
    private FirebaseFirestore fStore = null;
    private FirebaseDatabase mDatabase = null;
    private FirebaseUser user = null;
    private FirebaseStorage firebaseStorage;
    private StorageReference storageReference;
    private String userId = null;

    private String notebookId, notebookName, noteId, noteTitle;
    private String noteContent, notebookOwnerID, sharedNotebookPermission;
    private Intent data;
    private Toolbar toolbar;
    private EditText editNoteTitle;
    private Button saveBtn, clearBtn;
    private String lockStatus = null;

    @VisibleForTesting
    final StrokeManager strokeManager = new StrokeManager();
    private ArrayAdapter<ModelLanguageContainer> languageAdapter;
    private DrawingView drawingView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_ink_main);
        toolbar = findViewById(R.id.my_toolbar);
        editNoteTitle = findViewById(R.id.handNoteTitle);
        saveBtn = findViewById(R.id.save_button);
        clearBtn = findViewById(R.id.clear_button);
        editNoteTitle.setEnabled(false);
        setSupportActionBar(toolbar);

        drawingView = findViewById(R.id.drawing_view);
        drawingView.setStrokeManager(strokeManager);

        // init firebase
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance();
        user = mAuth.getCurrentUser();
        fStore = FirebaseFirestore.getInstance();
        firebaseStorage = FirebaseStorage.getInstance();
        storageReference = firebaseStorage.getReference();

        //strokeManager.setStatusChangedListener(statusTextView);
        strokeManager.setContentChangedListener(drawingView);
        strokeManager.setDownloadedModelsChangedListener(this);
        strokeManager.setClearCurrentInkAfterRecognition(true);
        strokeManager.setTriggerRecognitionAfterInput(false);

        languageAdapter = populateLanguageAdapter();
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        strokeManager.refreshDownloadedModelsStatus();

        strokeManager.reset();

        // Get notebook ID and notebook name from the intent extras
        data = getIntent();
        //extract data
        //String noteTitle = data.getStringExtra("title");
        if (data != null) {
            notebookId = data.getStringExtra("notebookID");
            //notebookName = data.getStringExtra("notebookName");
            noteId = data.getStringExtra("noteId");
            noteTitle = data.getStringExtra("noteTitle");
            noteContent = data.getStringExtra("contentNow");
            notebookOwnerID = data.getStringExtra("shareNoteOwnerID");
            sharedNotebookPermission = data.getStringExtra("permissions");

            // set screen title
            editNoteTitle.setText(noteTitle);

            if (notebookOwnerID != null) {
                // set userID to ownerID for shared notebooks to get notebook data
                Log.w(TAG, "Shared NoteBk owner ID = " + notebookOwnerID);
                userId = notebookOwnerID;

                // check for permissions level ---viewer or editor
                if (sharedNotebookPermission != null) {
                    //good
                    if (sharedNotebookPermission.matches("Viewer")) {
                        Log.w(TAG, "Shared NoteBk is = Viewer access");
                        /**
                         * shared notebook viewer level access
                         * set viewer-level stuff here
                         * like initializing, viewer level methods
                         */

                    } else {
                        Log.w(TAG, "Shared NoteBk is = Editor access");
                        /**
                         * shared notebook Editor level access
                         * set Editor-level stuff here
                         * like initializing, Editor level methods
                         */

                    }
                }
            } else {
                // default user
                Log.w(TAG, "failed to get shared NoteBook data.");
                userId = user.getUid();
            }
        } else {
            // default user
            userId = user.getUid();
            editNoteTitle.setText("");
            Log.w(TAG, "Error intent, no NoteBook data. ");
        }

        // set canvas with current note data
        getNoteData(userId, notebookId, noteId);

        // get notebook lock status
        getNotebookLockStatus(userId, notebookId);

        // clear screen
        clearBtn.setOnClickListener(e -> clearScreen());

        // save note
        saveBtn.setOnClickListener(e -> saveNote());

    }

    /**
     * activity lifecycle
     * refer to: <a href="https://developer.android.com/guide/components/activities/activity-lifecycle">...</a>
     */
    @Override
    protected void onStart() {
        super.onStart();
        // set canvas with current note data
        //getNoteData(userId, notebookId, noteId);
    }

    /**
     * activity lifecycle
     * refer to: <a href="https://developer.android.com/guide/components/activities/activity-lifecycle">...</a>
     */
    @Override
    protected void onResume() {
        super.onResume();
        // set canvas with current note data
        //getNoteData(userId, notebookId, noteId);
    }

    /**
     * onClick method called by the Download button.
     */
    public void downloadClick(View v) {
        strokeManager.download();
    }

    /**
     * onClick method called by the Recognize button.
     */
    private void recognizeScreenContent() {
        strokeManager.recognize();
    }

    private void saveNote() {
        // check lock status
        if (lockStatus.matches("No")) {
            // not locked, can save
            //create bitmap from canvas to save as note
            final Bitmap bitmap = getBitmapFromViewUsingCanvas(drawingView);
            saveBitmapToDB(userId, notebookId, noteId, bitmap);

        } else {
            // notebook is locked
            //can't save/add to it
            Toast.makeText(DigitalInkMainActivity.this, "This notebook is Locked.", Toast.LENGTH_SHORT).show();
        }
    }

    // save this entry/note to Firebase realtime DB
    // params: String title --> new note title
    // params: String Content --> new note content
    private void updateNoteInRealtimeDB(String title, String content) {
        // update note to realtime db
        try {
            String notebookID = "/" + notebookId + "/";

            Note note = new Note(title, content);
            // needs to be an update

            // Structure
            // entries > userID >> notebookID >> note entry/page_ID >> content to save
            mDatabase.getReference("entries/" + userId +
                    notebookID + noteId).setValue(note).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void aVoid) {
                    Log.w(TAG, "Successfully saved note info..");
                    Toast.makeText(DigitalInkMainActivity.this, "Note saved successfully!", Toast.LENGTH_SHORT).show();

                    // update notebook date stamp
                    updateNotebookStamp(notebookId);

                    // also update the note date modified in firestore db
                    // pages > userID >> notes >> notebookID >> entries >> created note_id
                    // not updating the title from here
                    final String currentDate = getCurrentDate();

                    fStore.collection("pages").document(userId)
                            .collection("notes").document(notebookId)
                            .collection("entries").document(noteId).update("lastModifiedDate", currentDate)
                            .addOnSuccessListener(e -> Log.w(TAG, "Successfully updated note date in firestore db"));

                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.w(TAG, "Failure to save note info.." + e);

                }
            });

        } catch (Exception e) {
            Log.w(TAG, e.toString());
        }
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

    /**
     * onClick method called by the Clear button.
     */
    private void clearScreen() {
        if (lockStatus.matches("No")) {
            // not locked, can save
            strokeManager.reset();
            DrawingView drawingView = findViewById(R.id.drawing_view);
            drawingView.clear();

        } else {
            // notebook is locked
            Toast.makeText(DigitalInkMainActivity.this, "This notebook is Locked.", Toast.LENGTH_SHORT).show();
        }
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
                                    // notebook is locked
                                    // disable canvas
                                    drawingView.setEnabled(false);
                                }
                                lockStatus = documentSnapshot.getString("isLocked");
                                //drawingView.setEnabled(true);
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

    /**
     * onClick method called by the Delete model button.
     */
    public void deleteClick(View v) {
        strokeManager.deleteActiveModel();
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
        if (item.getItemId() == R.id.closeBtn) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Input: bitmap
     * saves an image(bitmap) to device
     * returns a File
     *
     */
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
        File file = new File(directory, noteId + "page2" + ".png");
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

    /**
     * saves an image(bitmap) to firebase storage
     *
     */
    private void saveBitmapToDB(String userId, String notebookID, String noteID, Bitmap bitmap) {
        // Create a reference to the file to add
        StorageReference pageRef = storageReference.child("entries/" +
                userId + "/" + notebookID + "/" + noteID + "/page2.png" );

        // Create a reference to the file to delete
        StorageReference deleteRef = storageReference.child("entries/" +
                userId + "/" + notebookID + "/" + noteID + "/page2.png" );

        // check if bitmap is not empty
        if (bitmap.getWidth() < 0 && bitmap.getHeight() < 0) {
            // canvas is empty
            // exit
            Toast.makeText(DigitalInkMainActivity.this, "Can't save empty note", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File newfile = saveBitmapToFile(bitmap);
            Uri imageUri = Uri.fromFile(newfile);
            Log.w(TAG, "file: " + imageUri);
            InputStream stream = new FileInputStream(newfile);

            UploadTask uploadTask = pageRef.putStream(stream);

            //check if file exists
            deleteRef.getDownloadUrl().addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    // file doesn't exist yet
                    // then save new /page2.png
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Handle unsuccessful uploads
                            Log.w(TAG, "Failed uploading page2 note ");
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                            Toast.makeText(DigitalInkMainActivity.this, "Success uploading page2 note ", Toast.LENGTH_SHORT).show();
                            Log.w(TAG, "Success uploading page2 note ");

                            // update notebook date stamp
                            updateNotebookStamp(notebookId);

                            // also update the note date modified in firestore db
                            // pages > userID >> notes >> notebookID >> entries >> note_id
                            final String currentDate = getCurrentDate();

                            fStore.collection("pages").document(userId)
                                    .collection("notes").document(notebookId)
                                    .collection("entries").document(noteId).update("lastModifiedDate", currentDate)
                                    .addOnSuccessListener(e -> Log.w(TAG, "Successfully updated note date in firestore db"));
                        }
                    });
                }
            }).addOnSuccessListener(new OnSuccessListener<Uri>() {
                @Override
                public void onSuccess(Uri uri) {
                    // delete this image before saving again
                    // only save --> on delete success
                    // Delete the current /page2.png
                    deleteRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // File deleted successfully
                            // then save new /page2.png
                            uploadTask.addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception exception) {
                                    // Handle unsuccessful uploads
                                    Log.w(TAG, "Failed uploading page2 note ");
                                }
                            }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                @Override
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, etc.
                                    Toast.makeText(DigitalInkMainActivity.this, "Success uploading page2 note ", Toast.LENGTH_SHORT).show();
                                    Log.w(TAG, "Success uploading page2 note ");

                                    // update notebook date stamp
                                    updateNotebookStamp(notebookId);

                                    // also update the note date modified in firestore db
                                    // pages > userID >> notes >> notebookID >> entries >> note_id
                                    final String currentDate = getCurrentDate();

                                    fStore.collection("pages").document(userId)
                                            .collection("notes").document(notebookId)
                                            .collection("entries").document(noteId).update("lastModifiedDate", currentDate)
                                            .addOnSuccessListener(e -> Log.w(TAG, "Successfully updated note date in firestore db"));
                                }
                            });
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Uh-oh, an error occurred!
                            // we shouldn't save anthing
                            // so do nothing
                        }
                    });
                }
            });

        } catch (IOException e) {
            Log.d(TAG, "Bad error: " + e);
        }

    }

    /**
     * get the image(bitmap) and set on the canvas
     * image/bitmap is this notes data
     *
     */
    private void getNoteData(String userId, String notebookID, String noteID) {
        StorageReference noteRef = storageReference.child("entries/" +
                userId + "/" + notebookID + "/" + noteID + "/page2.png" );

        noteRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
            @SuppressLint("WrongCall")
            @Override
            public void onSuccess(Uri uri) {
                Log.w(TAG, "Success in getting note image. ");

                Glide.with(DigitalInkMainActivity.this).asBitmap().load(uri).into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        // set this bitmap to the drawingView canvas
                        drawingView.setDrawCanvas(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        /**
                         * nothing here, left empty
                         *
                         */
                    }
                });

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Error getting signature." + e);
            }
        });
    }

    /**
     * creates an image(bitmap) of the canvas
     *
     */
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

    private static class ModelLanguageContainer implements Comparable<ModelLanguageContainer> {
        private final String label;
        @Nullable
        private final String languageTag;
        private boolean downloaded;

        private ModelLanguageContainer(String label, @Nullable String languageTag) {
            this.label = label;
            this.languageTag = languageTag;
        }

        /**
         * Populates and returns a real model identifier, with label, language tag and downloaded
         * status.
         */
        public static ModelLanguageContainer createModelContainer(String label, String languageTag) {
            // Offset the actual language labels for better readability
            return new ModelLanguageContainer(label, languageTag);
        }

        /**
         * Populates and returns a label only, without a language tag.
         */
        public static ModelLanguageContainer createLabelOnly(String label) {
            return new ModelLanguageContainer(label, null);
        }

        public String getLanguageTag() {
            return languageTag;
        }

        public void setDownloaded(boolean downloaded) {
            this.downloaded = downloaded;
        }

        @NonNull
        @Override
        public String toString() {
            if (languageTag == null) {
                return label;
            } else if (downloaded) {
                return "   [D] " + label;
            } else {
                return "   " + label;
            }
        }

        @Override
        public int compareTo(ModelLanguageContainer o) {
            return label.compareTo(o.label);
        }
    }

    @Override
    public void onDownloadedModelsChanged(Set<String> downloadedLanguageTags) {
        for (int i = 0; i < languageAdapter.getCount(); i++) {
            ModelLanguageContainer container = languageAdapter.getItem(i);
            container.setDownloaded(downloadedLanguageTags.contains(container.languageTag));
        }
        languageAdapter.notifyDataSetChanged();
    }

    private ArrayAdapter<ModelLanguageContainer> populateLanguageAdapter() {
        ArrayAdapter<ModelLanguageContainer> languageAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_spinner_item);
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Select language"));
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Non-text Models"));

        // Manually add non-text models first
        for (String languageTag : NON_TEXT_MODELS.keySet()) {
            languageAdapter.add(
                    ModelLanguageContainer.createModelContainer(
                            NON_TEXT_MODELS.get(languageTag), languageTag));
        }
        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Text Models"));

        ImmutableSortedSet.Builder<ModelLanguageContainer> textModels =
                ImmutableSortedSet.naturalOrder();
        for (DigitalInkRecognitionModelIdentifier modelIdentifier :
                DigitalInkRecognitionModelIdentifier.allModelIdentifiers()) {
            if (NON_TEXT_MODELS.containsKey(modelIdentifier.getLanguageTag())) {
                continue;
            }
            if (modelIdentifier.getLanguageTag().endsWith(GESTURE_EXTENSION)) {
                continue;
            }

            textModels.add(buildModelContainer(modelIdentifier, "Script"));
        }
        languageAdapter.addAll(textModels.build());

        languageAdapter.add(ModelLanguageContainer.createLabelOnly("Gesture Models"));

        ImmutableSortedSet.Builder<ModelLanguageContainer> gestureModels =
                ImmutableSortedSet.naturalOrder();
        for (DigitalInkRecognitionModelIdentifier modelIdentifier :
                DigitalInkRecognitionModelIdentifier.allModelIdentifiers()) {
            if (!modelIdentifier.getLanguageTag().endsWith(GESTURE_EXTENSION)) {
                continue;
            }

            gestureModels.add(buildModelContainer(modelIdentifier, "Script gesture classifier"));
        }
        languageAdapter.addAll(gestureModels.build());
        return languageAdapter;
    }

    private static ModelLanguageContainer buildModelContainer(
            DigitalInkRecognitionModelIdentifier modelIdentifier, String labelSuffix) {
        StringBuilder label = new StringBuilder();
        label.append(new Locale(modelIdentifier.getLanguageSubtag()).getDisplayName());
        if (modelIdentifier.getRegionSubtag() != null) {
            label.append(" (").append(modelIdentifier.getRegionSubtag()).append(")");
        }

        if (modelIdentifier.getScriptSubtag() != null) {
            label.append(", ").append(modelIdentifier.getScriptSubtag()).append(" ").append(labelSuffix);
        }
        return ModelLanguageContainer.createModelContainer(
                label.toString(), modelIdentifier.getLanguageTag());
    }
}

