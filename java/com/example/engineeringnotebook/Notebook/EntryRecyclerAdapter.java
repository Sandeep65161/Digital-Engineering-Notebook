package com.example.engineeringnotebook.Notebook;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.engineeringnotebook.Model.InitialNote;
import com.example.engineeringnotebook.Note.EditNote;
import com.example.engineeringnotebook.R;

import java.util.List;

public class EntryRecyclerAdapter extends RecyclerView.Adapter<EntryRecyclerAdapter.EntryViewHolder> {
    Context mContext;
    private List<InitialNote> dataList;
    private final String notebookID;
    private final String notebookOwnerID;
    private final String sharedNotebookPermission;
    private final String lockStatus;

    public EntryRecyclerAdapter(Context context, List<InitialNote> list, String notebookID,
                                String notebookOwnerID, String sharedNotebookPermission, String lockStatus) {
        this.mContext = context;
        this.dataList = list;
        this.notebookID = notebookID;
        this.notebookOwnerID = notebookOwnerID;
        this.sharedNotebookPermission = sharedNotebookPermission;
        this.lockStatus = lockStatus;
    }

    @NonNull
    @Override
    public EntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout
        final View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notebook_entry, parent, false);

        return new EntryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EntryViewHolder holder, final int position) {
        if (dataList != null) {
            //int totalItems = getItemCount();
            final InitialNote currentNoteItem = dataList.get(position);

            if (currentNoteItem != null) {
                int pageNumber = position + 1;

                // noteID is important for saving this note's content to realtime db
                String noteID = currentNoteItem.getNoteID();
                String noteTitle = currentNoteItem.getTitle();
                String noteCreatedDate = currentNoteItem.getDateCreated();
                String noteLastModifiedDate = currentNoteItem.getLastModifiedDate();

                holder.getPageTextView().setText(String.valueOf(pageNumber));
                holder.getTitleTxt().setText(noteTitle);
                holder.getEntryDateCreatedTxt().setText(noteCreatedDate);
                holder.getLastModifiedTxt().setText(noteLastModifiedDate);

                // got to that note in Add/Edit Note activity when clicked
                holder.getView().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        // need to pass the clicked on note ID to next screen
                        // for saving in realtime db
                        Intent intent = new Intent(mContext.getApplicationContext(), EditNote.class);
                        intent.putExtra("notebookID", notebookID);
                        intent.putExtra("noteId", noteID);
                        intent.putExtra("noteTitle", noteTitle);
                        intent.putExtra("shareNoteOwnerID", notebookOwnerID);
                        intent.putExtra("permissions", sharedNotebookPermission);
                        intent.putExtra("isLocked", lockStatus);
                        mContext.startActivity(intent);
                    }
                });
            }
        }
    }

    public void setItemsList(List<InitialNote> list) {
        this.dataList = list;
    }

    @Override
    public int getItemCount() {
        int items = 0;
        if (dataList != null) items = dataList.size();
        return items;
    }

    private void setMargins (View view, int left, int top, int right, int bottom) {
        if (view.getLayoutParams() instanceof ViewGroup.MarginLayoutParams) {
            ViewGroup.MarginLayoutParams p = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            p.setMargins(left, top, right, bottom);
            view.requestLayout();
        }
    }

    public static class EntryViewHolder extends RecyclerView.ViewHolder {
        private final TextView pageTextView;
        private final TextView titleTxt;
        private final TextView entryDateCreatedTxt;
        private final TextView lastModifiedTxt;
        private final View view;

        public EntryViewHolder(@NonNull View itemView) {
            super(itemView);

            pageTextView = itemView.findViewById(R.id.entry_id);
            titleTxt = itemView.findViewById(R.id.entry_title);
            entryDateCreatedTxt = itemView.findViewById(R.id.entry_date_created);
            lastModifiedTxt = itemView.findViewById(R.id.entry_last_modified);
            view = itemView;
        }

        public TextView getPageTextView() {
            return pageTextView;
        }

        public TextView getTitleTxt() {
            return titleTxt;
        }

        public TextView getEntryDateCreatedTxt() {
            return entryDateCreatedTxt;
        }

        public TextView getLastModifiedTxt() {
            return lastModifiedTxt;
        }

        public View getView() {
            return view;
        }
    }
}


