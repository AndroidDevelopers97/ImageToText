package com.ad97.imagetotext;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SavedFilesActivity extends AppCompatActivity {


    /**
     * recycler view to list the files user saved in pdf format
     */
    private RecyclerView filesRecyclerView;
    /**
     * list to store file paths of the pdf file to open or share with someone
     */
    private List<String> filePaths;
    /**
     * path of the directory used by the application to store pdf
     */
    private String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/imageToText";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_files);
        filesRecyclerView = findViewById(R.id.files_rv);
        File dir = new File(path);
        if (!dir.exists())
            dir.mkdir();
        setFiles();
    }

    /**
     * method for setting list of files saved by user in recycler view
     */
    @SuppressLint("SimpleDateFormat")
    public void setFiles() {
        List<FileDetails> fileDetailsList = new ArrayList<>();
        filePaths = new ArrayList<>();
        File directory = new File(path);
        File[] files = directory.listFiles();
        if (files.length == 0) {
            findViewById(R.id.message).setVisibility(View.VISIBLE);
            filesRecyclerView.setVisibility(View.GONE);
        }
        else {
            findViewById(R.id.message).setVisibility(View.GONE);
            filesRecyclerView.setVisibility(View.VISIBLE);
            for (File file : files) {
                long fileSizeInBytes = file.length();
                // Convert the bytes to Kilobytes (1 KB = 1024 Bytes)
                long fileSizeInKB = fileSizeInBytes / 1024;
                // Convert the KB to MegaBytes (1 MB = 1024 KBytes)
                long fileSizeInMB = fileSizeInKB / 1024;

                Date date = new Date(file.lastModified());
                DateFormat df = new SimpleDateFormat("dd/MM/yy hh:mm a");
                if (fileSizeInMB > 0)
                    fileDetailsList.add(new FileDetails(file.getName(), "Size: " + fileSizeInMB + " MB", "Last Modified: " + df.format(date)));
                else if (fileSizeInKB > 0)
                    fileDetailsList.add(new FileDetails(file.getName(), "Size: " + fileSizeInKB + " kB", "Last Modified: " + df.format(date)));
                else
                    fileDetailsList.add(new FileDetails(file.getName(), "Size: " + fileSizeInBytes + " bytes", "Last Modified: " + df.format(date)));
                filePaths.add(Environment.getExternalStorageDirectory().getAbsolutePath() + "/imageToText/" + file.getName());
            }
            filesRecyclerView.setAdapter(new FilesAdapter(this, fileDetailsList));
            filesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
            filesRecyclerView.setHasFixedSize(true);
            filesRecyclerView.addOnItemTouchListener(new SavedFilesActivity.RecyclerTouchListener(this, filesRecyclerView, new ClickListener() {
                @Override
                public void onClick(View v, int pos) {
                    v.setSelected(v.isSelected());
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri data = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileProvider",
                            new File(filePaths.get(pos)));
                    intent.setDataAndType(data, "application/pdf");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(Intent.createChooser(intent, "Choose One Application To Open File"));
                    } catch (Exception e) {
                        Toast.makeText(SavedFilesActivity.this, "No PdfViewer To Open File", Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onLongClick(View v, final int pos) {
                    final String[] options = {"Share File", "Delete File"};
                    AlertDialog.Builder choiceBuilder = new AlertDialog.Builder(SavedFilesActivity.this);
                    choiceBuilder.setItems(options, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (options[which].equals("Share File")) {
                                Intent intent = new Intent(Intent.ACTION_SEND);
                                Uri data = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileProvider",
                                        new File(filePaths.get(pos)));
                                intent.putExtra(Intent.EXTRA_STREAM,data);
                                intent.setType("application/pdf");
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                try {
                                    startActivity(Intent.createChooser(intent, "Share file with"));
                                } catch (Exception e) {
                                    Toast.makeText(SavedFilesActivity.this, "Unable to share this file", Toast.LENGTH_LONG).show();
                                }
                            } else {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(SavedFilesActivity.this);

                                builder.setTitle("Delete File");

                                builder.setMessage("Are you sure to delete this file?");
                                builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        File file = new File(filePaths.get(pos));
                                        if(file.delete())
                                            Toast.makeText(getApplicationContext(),"File Deleted",Toast.LENGTH_LONG).show();
                                        setFiles();
                                    }
                                });
                                builder.setNegativeButton("No", null);
                                builder.setCancelable(false);
                                builder.show();
                            }
                        }
                    });
                    choiceBuilder.show();
                }
            }));
        }
    }

    /**
     * interface for the onClick and onLongClick for recycler view
     */
    public interface ClickListener {
        void onClick(View v, int pos);

        void onLongClick(View v, int pos);
    }

    /**
     * class implementing OnItemTouchListener for add item click listener on recycler view
     */
    class RecyclerTouchListener implements RecyclerView.OnItemTouchListener {

        private GestureDetector gestureDetector;
        private ClickListener clickListener;
        private RecyclerView recyclerView;

        RecyclerTouchListener(Context context, final RecyclerView recyclerView, final ClickListener clickListener) {

            this.clickListener = clickListener;
            this.recyclerView = recyclerView;

            gestureDetector = new GestureDetector(context, new GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    return true;
                }

                @Override
                public void onLongPress(MotionEvent e) {
                    View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
                    if (child != null && clickListener != null) {
                        clickListener.onLongClick(child, recyclerView.getChildPosition(child));
                    }
                }
            });
        }

        @Override
        public boolean onInterceptTouchEvent(RecyclerView rv, MotionEvent e) {
            View child = recyclerView.findChildViewUnder(e.getX(), e.getY());
            if (child != null && clickListener != null && gestureDetector.onTouchEvent(e)) {
                clickListener.onClick(child, rv.getChildPosition(child));
                return false;
            }

            return false;
        }

        @Override
        public void onTouchEvent(RecyclerView rv, MotionEvent e) {

        }

        @Override
        public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {

        }
    }
}
