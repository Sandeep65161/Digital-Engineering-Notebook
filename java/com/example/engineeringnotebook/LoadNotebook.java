package com.example.engineeringnotebook;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.example.engineeringnotebook.Model.Notebook;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class LoadNotebook extends AppCompatActivity {

    //instance of Firebase Firestore
    //for interaction with db
    FirebaseFirestore fStore;
    //instance of Firebase Authentication
    //for user authentication
    FirebaseAuth fAuth;
    //list to store notebook objects retrieved from Firestore
    ArrayList<Notebook> notebookList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_load_notebook);
        getSupportActionBar().setTitle("Select a Notebook");
        //add back button to load notebooks screen
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fStore = FirebaseFirestore.getInstance();
        fAuth = FirebaseAuth.getInstance();
        notebookList = new ArrayList<>();

        //set up RecyclerView to display notebooks
        RecyclerView recyclerView = findViewById(R.id.sharedNotebookList);
        //set orientation of displayed notes
        recyclerView.setLayoutManager(new StaggeredGridLayoutManager(5, StaggeredGridLayoutManager.VERTICAL));
        //instance of adapter
        NotebookAdapter adapter = new NotebookAdapter(fAuth.getCurrentUser().getUid());
        recyclerView.setAdapter(adapter);

        if (fAuth.getCurrentUser() != null){
            //retrieve user's notebook data from Firebase
            fStore.collection("users")
                    .document(fAuth.getCurrentUser().getUid())
                    .collection("notebooks")
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                //convert Firestore document to Notebook object and add to notebookList
                                Notebook notebook = document.toObject(Notebook.class);
                                notebookList.add(notebook);
                            }
                            //notify adapter of data change
                            adapter.notifyDataSetChanged();
                        }
                    });
        }
    }

    //custom RecyclerView adapter to display notebooks
    //create an instance of notebook adapter
    private class NotebookAdapter extends RecyclerView.Adapter<NotebookAdapter.NotebookViewHolder> {
        final private String userId;

        public NotebookAdapter(String userId) {
            this.userId = userId;
        }

        @NonNull
        @Override
        public NotebookViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            //inflate the notebook_view layout for each item
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.notebook_view, parent, false);
            return new NotebookViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull NotebookViewHolder holder, int position) {
            //bind notebook data to the ViewHolder at the specified position
            Notebook notebook = notebookList.get(position);
            holder.bind(notebook);
        }

        @Override
        public int getItemCount() {
            //return the total number of notebooks in the list
            return notebookList.size();
        }

        //viewHolder class for notebook items
        class NotebookViewHolder extends RecyclerView.ViewHolder {
            TextView title, createdDate, createdTime, nbStatus, lastModified;

            public NotebookViewHolder(@NonNull View itemView) {
                super(itemView);
                //initialize views within the notebook_view layout
                title = itemView.findViewById(R.id.sharedNbTitle);
                createdDate = itemView.findViewById(R.id.sharedNbCreatedDate);
                createdTime = itemView.findViewById(R.id.dateNbShared);
                nbStatus = itemView.findViewById(R.id.nbStatus);
                lastModified = itemView.findViewById(R.id.sharedNbDateLastMod);
            }

            @SuppressLint("SetTextI18n")
            public void bind(Notebook notebook) {
                //display both date and time in the createdDateTime
                //String dateTime = notebook.getDateCreated() + " " + notebook.getTimeCreated();

                //bind notebook data to the views
                title.setText(notebook.getName());
                createdDate.setText(notebook.getDateCreated());
                createdTime.setText(notebook.getTimeCreated());
                if (notebook.getIsLocked() != null){
                    if(notebook.getIsLocked().equals("Yes")){
                        nbStatus.setText("Locked & Signed Off");
                    } else{
                        nbStatus.setText("Unlocked");
                    }
                }

                // get and set last modified date
                try {
                    // Structure
                    // stamp > userID >> notebookID >> stamp54YgFGl = same document(ID) for all
                    fStore.collection("stamp").document(userId)
                            .collection(notebook.getId())
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

                //handle click listening for notebook items
                //launch main activity on click of notebook
                itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(LoadNotebook.this, MainActivity.class);
                    //pass notebook details to MainActivity
                    intent.putExtra("notebookId", notebook.getId());
                    intent.putExtra("notebookName", notebook.getName());
                    intent.putExtra("notebookDateCreated", notebook.getDateCreated());
                    intent.putExtra("notebookTimeCreated", notebook.getTimeCreated());
                    intent.putExtra("dateCreated", notebook.getDateCreated());
                    intent.putExtra("isLocked", notebook.getIsLocked());
                    startActivity(intent);
                });
            }
        }
    }

    //handle click on back button
    //on click send user to prior activity
   @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        onBackPressed();
        return super.onOptionsItemSelected(item);
    }
}
