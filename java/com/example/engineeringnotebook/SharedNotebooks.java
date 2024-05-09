package com.example.engineeringnotebook;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.engineeringnotebook.Model.SharedNbStructure;
import com.example.engineeringnotebook.utils.LogHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class SharedNotebooks extends AppCompatActivity {
    private static final String TAG = LogHelper.makeLogTag(SharedNotebooks.class);
    private RecyclerView sharedNotebooksRecyclerView;
    //list to store notebook objects retrieved from Firestore
    private ArrayList<SharedNbStructure> sharedNotebooksList;
    private SharedNotebookAdapter adapter;
    private FirebaseDatabase database = null;
    private FirebaseUser currentUser = null;

    private FirebaseFirestore fStore;
    //instance of Firebase Authentication
    //for user authentication
    private FirebaseAuth fAuth;
    private CardView cardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_notebooks);
        getSupportActionBar().setTitle("Select Shared Notebook");
        //add back button to load notebooks screen
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();

        //set up RecyclerView to display notebooks
        sharedNotebooksRecyclerView = findViewById(R.id.sharedNotebookList);
        sharedNotebooksList = new ArrayList<>();

        //create a reference to the Realtime Database
        database = FirebaseDatabase.getInstance();

        //set orientation of displayed notes
        sharedNotebooksRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(5, StaggeredGridLayoutManager.VERTICAL));

        //instance of adapter
        adapter = new SharedNotebookAdapter(sharedNotebooksList);
        sharedNotebooksRecyclerView.setAdapter(adapter);

        //get currently logged-in user
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        //get shared notebooks
        loadSharedNotebooks();
    }

    private void loadSharedNotebooks() {
        if (currentUser != null) {
            //String userId = currentUser.getUid();
            String userEmail = currentUser.getEmail();
            String[] parts = userEmail.split("@");
            String uEmail = parts[0];

            DatabaseReference sharedNotebooksRef = database.getReference("SharedNotebooks")
                    .child(uEmail); // Use the user's email or user ID as a child node
            sharedNotebooksRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    sharedNotebooksList.clear();
                    for (DataSnapshot sharedNotebookSnapshot : dataSnapshot.getChildren()) {
                        // Each child represents a unique shared notebook key
                        String sharedNotebookKey = sharedNotebookSnapshot.getKey();
                        if (sharedNotebookKey != null) {
                            // Retrieve data from the snapshot
                            String title = sharedNotebookSnapshot.child("name").getValue(String.class);
                            String dateCreated = sharedNotebookSnapshot.child("dateCreated").getValue(String.class);
                            String sharedNbID = sharedNotebookSnapshot.child("shareNbID").getValue(String.class);
                            String ownerID = sharedNotebookSnapshot.child("ownerID").getValue(String.class);
                            String recipientEmail = sharedNotebookSnapshot.child("email").getValue(String.class);
                            String dateShared = sharedNotebookSnapshot.child("dateShared").getValue(String.class);
                            String permissions = sharedNotebookSnapshot.child("permissions").getValue(String.class);
                            String UniqueSharedNBID = sharedNotebookSnapshot.child("uniqueSharedNBID").getValue(String.class);
                            String revoked = sharedNotebookSnapshot.child("revoked").getValue(String.class);

                            // Create a shared notebook object
                            SharedNbStructure sharedNotebook = new SharedNbStructure(title, recipientEmail, UniqueSharedNBID, sharedNbID, ownerID, dateCreated, dateShared, permissions, revoked);
                            //Log.w(TAG, "NoteBook id = " + sharedNotebook.getShareNbID() + " ownerID = " + sharedNotebook.getOwnerID() + " Unique Notebook ID =" + UniqueSharedNBID);
                            sharedNotebooksList.add(sharedNotebook);
                        }
                    }
                    //notify the adapter of data change
                    adapter.setNotebooks(sharedNotebooksList);
                    adapter.notifyDataSetChanged();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    //handle database error
                }
            });
        }
    }

    private void getSharedNotebookLockStatus(String ownerID, String sharedNbID, LockStatusCallback callback) {
        if (ownerID != null && sharedNbID != null) {
            //retrieve shared notebook data from Firestore
            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
            DocumentReference sharedNotebookRef = firestore.collection("users")
                    .document(ownerID)
                    .collection("notebooks")
                    .document(sharedNbID);

            sharedNotebookRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists() && documentSnapshot.contains("isLocked")) {
                    String lockStatus = documentSnapshot.getString("isLocked");
                    callback.onLockStatusChanged(lockStatus);
                } else {
                    callback.onLockStatusChanged("No");
                }
            }).addOnFailureListener(e -> {
                Log.d(TAG, "Shared Notebook lock status query error: " + e.getMessage());
                callback.onLockStatusChanged("No");
            });
        } else {
            callback.onLockStatusChanged("No");
        }
    }

    //define LockStatusCallback
    public interface LockStatusCallback {
        void onLockStatusChanged(String lockStatus);
    }

    //recyclerView Adapter
    public class SharedNotebookAdapter extends RecyclerView.Adapter<SharedNotebookAdapter.SharedNotebookViewHolder> {

        private ArrayList<SharedNbStructure> sharedNotebooksList;
        //new list for displayed items
        private ArrayList<SharedNbStructure> displayedNotebooksList;

        public SharedNotebookAdapter(ArrayList<SharedNbStructure> sharedNotebooksList) {
            this.sharedNotebooksList = sharedNotebooksList;
            //initially display all items
            this.displayedNotebooksList = new ArrayList<>(sharedNotebooksList);
        }

        public void setNotebooks(ArrayList<SharedNbStructure> sharedNotebooksList) {
            this.sharedNotebooksList = sharedNotebooksList;
            this.displayedNotebooksList = new ArrayList<>(sharedNotebooksList); // Set and update both lists
            //filter hidden notebooks
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SharedNotebookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            final View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.shared_notebook_view, parent, false);
            return new SharedNotebookViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SharedNotebookViewHolder holder, int position) {
            SharedNbStructure sharedNotebook = displayedNotebooksList.get(position);
            holder.bind(sharedNotebook);

            //check if the title should be red based on the saved state
            SharedPreferences coloredNBpreference = holder.itemView.getContext().getSharedPreferences("NotebookColors", Context.MODE_PRIVATE);
            int clickedPosition = coloredNBpreference.getInt("clicked_notebook_position_" + sharedNotebook.getUniqueSharedNBID(), -1); //get stored position uniquely
            if (clickedPosition == position) {
                holder.titleTextView.setTextColor(Color.RED);
            } else{
                holder.titleTextView.setTextColor(Color.BLUE);
            }
        }

        @Override
        public int getItemCount() {
            return displayedNotebooksList.size();
        }

        //viewholder for shared notebooks
        public class SharedNotebookViewHolder extends RecyclerView.ViewHolder {
            private PopupMenu menu = null;
            private ImageView menuIcon = itemView.findViewById(R.id.menuIcon);
            private ConstraintLayout constraintLayout;
            private TextView titleTextView, dateTextView, date2TextView, permissionsTextView, lastModified;

            public SharedNotebookViewHolder(@NonNull View itemView) {
                super(itemView);
                titleTextView = itemView.findViewById(R.id.sharedNbTitle);
                dateTextView = itemView.findViewById(R.id.sharedNbCreatedDate);
                date2TextView = itemView.findViewById(R.id.dateNbShared);
                permissionsTextView = itemView.findViewById(R.id.nbStatus);
                lastModified = itemView.findViewById(R.id.sharedNbDateLastMod);
            }

            public void bind(SharedNbStructure sharedNb) {
                titleTextView.setText(sharedNb.getName());
                dateTextView.setText(sharedNb.getDateCreated());
                date2TextView.setText(sharedNb.getDateShared());
                permissionsTextView.setText(sharedNb.getPermissions());

                // get and set last modified date
                try {
                    // Structure
                    // stamp > userID >> notebookID >> stamp54YgFGl = same document(ID) for all
                    fStore.collection("stamp").document(sharedNb.getOwnerID())
                            .collection(sharedNb.getShareNbID())
                            .document("stamp54YgFGl")
                            .get()
                            .addOnSuccessListener(documentReference -> {
                                Log.w(TAG, "Successfully got notebook date stamp");
                                String lastModifiedDate = documentReference.getString("lastModifiedDate");

                                lastModified.setText(lastModifiedDate);

                            })
                            .addOnFailureListener(e -> {
                                // Handle the case where creating the notebook document failed
                                Log.w(TAG, "Failed to get last modified date: " + e.getMessage());
                            });
                } catch (Exception e) {
                    Log.w(TAG, "Failed to get last modified date: " + e);
                }

                SharedPreferences hidePreference = itemView.getContext().getSharedPreferences("NotebookHidden", Context.MODE_PRIVATE);
                boolean isHidden = hidePreference.getBoolean(sharedNb.getUniqueSharedNBID() + "_hidden_", false);
                itemView.setVisibility(isHidden ? View.GONE : View.VISIBLE);

                //handle click listening for notebook items
                //launch main activity on click of notebook
                itemView.setOnClickListener(v -> {
                    getSharedNotebookLockStatus(sharedNb.getOwnerID(), sharedNb.getShareNbID(), new LockStatusCallback() {
                        @Override
                        public void onLockStatusChanged(String lockStatus) {
                            Log.w(TAG, "lock status at method call: " + lockStatus);

                            if ("Yes".equals(sharedNb.getRevoked())) {
                                titleTextView.setTextColor(Color.RED);
                                //change the text color to red
                                titleTextView.setTextColor(Color.RED);
                                //save the color change state to SharedPreferences when "Done" is clicked
                                SharedPreferences sharedPreferences = itemView.getContext().getSharedPreferences("NotebookColors", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean(sharedNb.getUniqueSharedNBID() + "_color_changed", true);
                                editor.putInt("clicked_notebook_position_" + sharedNb.getUniqueSharedNBID(), getAdapterPosition()); // Store the position uniquely
                                editor.apply();

                                // Update the UI immediately
                                notifyDataSetChanged();
                                Toast.makeText(SharedNotebooks.this, "Request Access to view Notebook", Toast.LENGTH_SHORT).show();
                            } else if ("Yes".equals(lockStatus)) {
                                titleTextView.setTextColor(Color.RED);
                                //change the text color to red
                                titleTextView.setTextColor(Color.RED);
                                //save the color change state to SharedPreferences when "Done" is clicked
                                SharedPreferences sharedPreferences = itemView.getContext().getSharedPreferences("NotebookColors", Context.MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putBoolean(sharedNb.getUniqueSharedNBID() + "_color_changed", true);
                                editor.putInt("clicked_notebook_position_" + sharedNb.getUniqueSharedNBID(), getAdapterPosition()); // Store the position uniquely
                                editor.apply();

                                // Update the UI immediately
                                notifyDataSetChanged();
                                Toast.makeText(SharedNotebooks.this, "Notebook Locked by Owner", Toast.LENGTH_SHORT).show();
                            } else {
                                //launch the main activity
                                Intent intent = new Intent(SharedNotebooks.this, MainActivity.class);

                                //pass notebook details to MainActivity
                                Log.w(TAG, "NoteBook id = " + sharedNb.getShareNbID() + " ownerID = " + sharedNb.getOwnerID());
                                intent.putExtra("notebookId", sharedNb.getShareNbID());
                                intent.putExtra("notebookName", sharedNb.getName());
                                intent.putExtra("ownerID", sharedNb.getOwnerID());
                                intent.putExtra("recipientEmail", sharedNb.getRecipientEmail());
                                intent.putExtra("notebookDateCreated", sharedNb.getDateCreated());
                                intent.putExtra("notebookTimeCreated", sharedNb.getDateShared());
                                intent.putExtra("permissions", sharedNb.getPermissions());
                                intent.putExtra(("uniqueSharedNBID"), sharedNb.getUniqueSharedNBID());
                                startActivity(intent);
                            }
                        }
                    });
                });

                menuIcon.setOnClickListener(view -> {
                    menu = new PopupMenu(itemView.getContext(), view);
                    menu.setGravity(Gravity.START);

                    menu.getMenu().add("Done").setOnMenuItemClickListener(menuItem -> {
                        //change the text color to red
                        titleTextView.setTextColor(Color.RED);
                        //save the color change state to SharedPreferences when "Done" is clicked
                        SharedPreferences sharedPreferences = itemView.getContext().getSharedPreferences("NotebookColors", Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putBoolean(sharedNb.getUniqueSharedNBID() + "_color_changed", true);
                        editor.putInt("clicked_notebook_position_" + sharedNb.getUniqueSharedNBID(), getAdapterPosition()); // Store the position uniquely
                        editor.apply();

                        // Update the UI immediately
                        notifyDataSetChanged();

                        if (currentUser != null){
                            //String userId = currentUser.getUid();
                            String userEmail = currentUser.getEmail();
                            String[] parts = userEmail.split("@");
                            String uEmail = parts[0];

                            DatabaseReference notebookRef = database.getReference("SharedNotebooks")
                                    .child(uEmail)
                                    .child(sharedNb.getUniqueSharedNBID())
                                    .child("revoked");

                            notebookRef.setValue("Yes").addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    //update the SharedNbStructure object directly
                                    sharedNb.setRevoked("Yes");
                                    notifyDataSetChanged();
                                    Toast.makeText(SharedNotebooks.this, "Access Revoked Successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(SharedNotebooks.this, "Failed to Revoke Access", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                        return false;
                    });
                    menu.getMenu().add("Delete").setOnMenuItemClickListener(menuItem -> {
                       if (!sharedNb.getRevoked().equals("Yes")) {
                            Toast.makeText(SharedNotebooks.this, "Need Access Revoked (Done) to Delete Notebook", Toast.LENGTH_SHORT).show();
                        } else {
                            //delete the notebook from the database
                            deleteNotebook(sharedNb);

                            //remove the notebook from the displayedNotebooksList and notify the adapter
                            int position = displayedNotebooksList.indexOf(sharedNb);
                            displayedNotebooksList.remove(position);
                            notifyItemRemoved(position);
                        }
                        return  false;
                    });
                    menu.show();
                });

                SharedPreferences coloredNBpreference = itemView.getContext().getSharedPreferences("NotebookColors", Context.MODE_PRIVATE);
                boolean isRed = coloredNBpreference.getBoolean(sharedNb.getUniqueSharedNBID() + "_color_changed", false);
                if (isRed) {
                   // constraintLayout.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                } else {
                   // constraintLayout.setBackgroundColor(Color.GRAY);
                }
            }
        }

        private void deleteNotebook(SharedNbStructure sharedNb) {
            if (currentUser != null) {
                String userEmail = currentUser.getEmail();
                String[] parts = userEmail.split("@");
                String uEmail = parts[0];

                DatabaseReference notebookRef = database.getReference("SharedNotebooks")
                        .child(uEmail)
                        .child(sharedNb.getUniqueSharedNBID());

                notebookRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        //check if the notebook is marked as "revoked"
                        if (dataSnapshot.exists() && dataSnapshot.child("revoked").getValue(String.class).equals("Yes")) {
                            notebookRef.removeValue().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    //notebook deleted successfully
                                    Toast.makeText(SharedNotebooks.this, "Deleted Successfully", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(SharedNotebooks.this, "Failed to Delete Notebook", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            //notebook is not marked as "revoked"
                            Toast.makeText(SharedNotebooks.this, "Notebook is not marked as revoked", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        }
    }

    //handle click on back button
    //on click reorder send user to prior activity
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        //send user to prior activity
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }
}
