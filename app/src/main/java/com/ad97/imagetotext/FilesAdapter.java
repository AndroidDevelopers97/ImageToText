package com.ad97.imagetotext;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class FilesAdapter extends RecyclerView.Adapter<FilesAdapter.FilesViewHolder> {

    private Context mContext;
    private List<FileDetails> fileDetailsList;

    FilesAdapter(Context mContext, List<FileDetails> fileDetailsList) {
        this.mContext = mContext;
        this.fileDetailsList = fileDetailsList;
    }

    @NonNull
    @Override
    public FilesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(mContext).inflate(R.layout.layout_files,parent,false);
        return new FilesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FilesViewHolder holder, int position) {
        FileDetails fileDetails= fileDetailsList.get(position);
        holder.fileName.setText(fileDetails.getFileName());
        holder.fileSize.setText(fileDetails.getSize());
        holder.dateAndTime.setText(fileDetails.getDateAndTime());
    }

    @Override
    public int getItemCount() {
        return fileDetailsList.size();
    }

     class FilesViewHolder extends RecyclerView.ViewHolder{

        private TextView fileName,fileSize,dateAndTime;

        FilesViewHolder(View view) {
            super(view);
            fileName = view.findViewById(R.id.fileName);
            fileSize = view.findViewById(R.id.size);
            dateAndTime = view.findViewById(R.id.dateAndTime);
        }
    }
}
