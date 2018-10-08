package com.ad97.imagetotext;

import android.app.Dialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.support.annotation.ColorInt;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import yuku.ambilwarna.AmbilWarnaDialog;

public class EditActivity extends AppCompatActivity {

    /**
     * editText is used for editing the scanned text from the image
     */
    private EditText editText;
    /**
     * undo and redo buttons to perform operations according to their names
     */
    private ImageButton undoButton, redoButton;
    /**
     * undo and redo list of spannable text to store the undo and redo history
     */
    private ArrayList<SpannableStringBuilder> undo, redo;
    /**
     * undo and redo list for the cursor position used to set cursor at a particular position according to undo/redo operations
     */
    private ArrayList<Integer> undoCursor, redoCursor;
    /**
     * spannedText is used for storing and editing spannable text edited by the used
     */
    private SpannableStringBuilder spannedText;
    private Boolean urClicked = false,ebClicked = false;
    private int cursor;
    /**
     * final variables donating particular action
     */
    private final int BOLD_ACTION = 1, ITALIC_ACTION = 2, UNDERLINE_ACTION = 3, COLOR_ACTION = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        undoButton = findViewById(R.id.undo);
        redoButton = findViewById(R.id.redo);
        editText = findViewById(R.id.editText);
        undoButton.setImageResource(R.drawable.undo_fade);
        redoButton.setImageResource(R.drawable.redo_fade);
        undoButton.setClickable(false);
        redoButton.setClickable(false);
        undo = new ArrayList<>();
        redo = new ArrayList<>();
        undoCursor = new ArrayList<>();
        redoCursor = new ArrayList<>();
        editText.setText(getIntent().getStringExtra("scanned_text"));
        spannedText = new SpannableStringBuilder(editText.getText());
        undo.add(spannedText);
        cursor = editText.getText().length() - 1;
        undoCursor.add(cursor);
        editText.setSelection(cursor);
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (!urClicked) {
                    if (undo.size() < 15) {
                        undo.add(new SpannableStringBuilder(editText.getText()));
                        undoCursor.add(cursor);
                    }
                    else if (undo.size() == 15) {
                        undo.remove(0);
                        undo.add(new SpannableStringBuilder(editText.getText()));
                        undoCursor.remove(0);
                        undoCursor.add(cursor);
                    }
                    redoButton.setImageResource(R.drawable.redo_fade);
                    undoButton.setImageResource(R.drawable.undo);
                    undoButton.setClickable(true);
                    redoButton.setClickable(false);
                    redo.clear();
                }
                cursor = editText.getText().length() - 1;
                urClicked = false;
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if(!ebClicked)
                    cursor = start;
                ebClicked = false;
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }
        });

        undoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                urClicked = true;
                redoButton.setImageResource(R.drawable.redo);
                redoButton.setClickable(true);
                int size = undo.size();
                if (size > 0) {
                    editText.setText(undo.get(size - 1));
                    editText.setSelection(undoCursor.get(size-1));
                    redo.add(undo.get(size - 1));
                    redoCursor.add(undoCursor.get(size-1));
                    undo.remove(size - 1);
                    undoCursor.remove(size-1);

                    if (undo.size() == 0) {
                        undoButton.setImageResource(R.drawable.undo_fade);
                        undoButton.setClickable(false);
                    }
                } /*else {
                    undoButton.setBackgroundResource(R.drawable.undo_redo_off_button);
                    undoButton.setClickable(false);
                }*/
               // editText.setSelection(editText.getText().length() - 1);
            }
        });

        redoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                urClicked = true;
                undoButton.setImageResource(R.drawable.undo);
                undoButton.setClickable(true);
                int size = redo.size();
                 if(size > 0) {
                    editText.setText(redo.get(size - 1));
                    editText.setSelection(redoCursor.get(size - 1));
                    undo.add(redo.get(size - 1));
                    redo.remove(size - 1);
                    undoCursor.add(redoCursor.get(size - 1));
                    redoCursor.remove(size - 1);
                    if (redo.size() == 0) {
                        redoButton.setImageResource(R.drawable.redo_fade);
                        redoButton.setClickable(false);
                    }
                }
                //editText.setSelection(editText.getText().length() - 1);
            }
        });
    }

    /**
     * bold the selection text onClick
     * @param v view of bold button
     */
    public void bold(View v) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if(start!=end)
            changeFont(start, end, BOLD_ACTION, 0);
    }

    /**
     * italic the selection text onClick
     * @param v view of italic button
     */
    public void italic(View v) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if(start!=end)
            changeFont(start, end, ITALIC_ACTION, 0);
    }

    /**
     * underline the selection text onClick
     * @param v view of underline button
     */
    public void underline(View v) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        if(start!=end)
            changeFont(start, end, UNDERLINE_ACTION, 0);
    }

    /**
     * color the selected color to the selection text onClick
     * @param v view of color button
     */
    public void color(View v) {

        final int start = editText.getSelectionStart();
        final int end = editText.getSelectionEnd();
        if(start!=end) {
            ForegroundColorSpan foregroundColorSpan[] = spannedText.getSpans(start, end, ForegroundColorSpan.class);
            int initialColor = Color.BLACK;
            if(foregroundColorSpan.length>0)
                initialColor = foregroundColorSpan[foregroundColorSpan.length-1].getForegroundColor();
            AmbilWarnaDialog dialog = new AmbilWarnaDialog(this,
                    initialColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
                @Override
                public void onCancel(AmbilWarnaDialog dialog) {
                }
                @Override
                public void onOk(AmbilWarnaDialog dialog, int selectedColor) {
                    changeFont(start, end, COLOR_ACTION, selectedColor);
                }
            });
            dialog.show();
        }
    }

    /**
     * copy the edited text to clipboard
     * @param v view of copy button
     */
    public void copy(View v) {

        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copy", editText.getText());
        if (clipboard != null)
            clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Copied to clipboard", Toast.LENGTH_LONG).show();
    }

    /**
     * method to change text according to action button (bold,italic,etc.)
     * @param start selection start
     * @param end selection end
     * @param action which action to perform
     * @param color color applied to text
     */
    private void changeFont(int start, int end, int action, @ColorInt int color) {

        spannedText = new SpannableStringBuilder(editText.getText());
        StyleSpan styleSpan[] = spannedText.getSpans(start, end, StyleSpan.class);
        StringBuilder typefaces = new StringBuilder();
        switch (action) {
            case BOLD_ACTION:
                if (styleSpan.length > 0) {
                    for (StyleSpan item : styleSpan) {
                        typefaces.append(item.getStyle());
                        spannedText.removeSpan(item);
                    }
                    if (typefaces.indexOf("1") > -1)
                        spannedText.removeSpan(styleSpan[typefaces.indexOf("1")]);
                    else
                        spannedText.setSpan(new StyleSpan(Typeface.BOLD), start, end, 0);
                    if (typefaces.indexOf("2") > -1)
                        spannedText.setSpan(new StyleSpan(Typeface.ITALIC), start, end, 0);
                } else
                    spannedText.setSpan(new StyleSpan(Typeface.BOLD), start, end, 0);
                break;
            case ITALIC_ACTION:
                if (styleSpan.length > 0) {
                    for (StyleSpan item : styleSpan) {
                        typefaces.append(item.getStyle());
                        spannedText.removeSpan(item);
                    }
                    if (typefaces.indexOf("2") > -1)
                        spannedText.removeSpan(styleSpan[typefaces.indexOf("2")]);
                    else
                        spannedText.setSpan(new StyleSpan(Typeface.ITALIC), start, end, 0);
                    if (typefaces.indexOf("1") > -1)
                        spannedText.setSpan(new StyleSpan(Typeface.BOLD), start, end, 0);
                } else
                    spannedText.setSpan(new StyleSpan(Typeface.ITALIC), start, end, 0);
                break;
            case UNDERLINE_ACTION:
                UnderlineSpan underlineSpan[] = spannedText.getSpans(start, end, UnderlineSpan.class);
                if (underlineSpan.length > 0)
                    spannedText.removeSpan(underlineSpan[0]);
                else
                    spannedText.setSpan(new UnderlineSpan(), start, end, 0);
                break;
            case COLOR_ACTION:
                ForegroundColorSpan foregroundColorSpan[] = spannedText.getSpans(start,end,ForegroundColorSpan.class);
                if(foregroundColorSpan.length>0)
                    spannedText.removeSpan(foregroundColorSpan[0]);
                spannedText.setSpan(new ForegroundColorSpan(color), start, end, 0);
                break;
        }
        cursor = start;
        ebClicked = true;
        editText.setText(spannedText);
        editText.setSelection(start, end);
    }

    /**
     * onClick method for saving the scanned and edited text of the image in pdf file
     * @param v view of save button
     */
    public void save(View v) {

        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.layout_save_file);
        dialog.setCancelable(false);
        final EditText file = dialog.findViewById(R.id.filename);
        Button save = dialog.findViewById(R.id.save);
        Button cancel = dialog.findViewById(R.id.cancel);
        save.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String filename = file.getText().toString().replaceAll("[^a-zA-Z0-9_]", "");
                if (filename.isEmpty())
                    Toast.makeText(getApplicationContext(), "Please Enter the Valid File Name to Save!", Toast.LENGTH_LONG).show();
                else
                    saveFile(filename);
                dialog.dismiss();
            }
        });

        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
                }
                });
        dialog.show();


    }

    /**
     * saving the scanned and edited text of the image in pdf file
     * @param filename filename entered by user
     */
    private void saveFile(String filename) {
        spannedText = new SpannableStringBuilder(editText.getText());
        Document document = new Document();
        Paragraph paragraph = new Paragraph();
        for (int i = 0; i < spannedText.length(); i++) {
            StringBuilder typefaces = new StringBuilder();
            StyleSpan styleSpan[] = spannedText.getSpans(i, i + 1, StyleSpan.class);
            UnderlineSpan underlineSpan[] = spannedText.getSpans(i, i + 1, UnderlineSpan.class);
            ForegroundColorSpan foregroundColorSpan[] = spannedText.getSpans(i, i + 1, ForegroundColorSpan.class);
            char ch = spannedText.charAt(i);
            int fontStyle = Font.NORMAL;
            boolean underline = underlineSpan.length > 0;
            Chunk chunk;
            if (styleSpan.length > 0) {
                for (StyleSpan item : styleSpan)
                    typefaces.append(item.getStyle());

                if (typefaces.indexOf("1") > -1 && typefaces.indexOf("2") > -1)
                    fontStyle = Font.BOLDITALIC;
                else if (typefaces.indexOf("1") > -1)
                    fontStyle = Font.BOLD;
                else if (typefaces.indexOf("2") > -1)
                    fontStyle = Font.ITALIC;
            }

            Font font = new Font(Font.FontFamily.HELVETICA, 12, fontStyle);
            if (foregroundColorSpan.length > 0)
                font.setColor(new BaseColor(foregroundColorSpan[0].getForegroundColor()));
            chunk = new Chunk(ch, font);
            if (underline)
                chunk.setUnderline(0.1f, -2f);
            paragraph.add(chunk);
        }
        paragraph.setAlignment(Paragraph.ALIGN_LEFT);
        String path;
        try {
            path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/imageToText";
            File dir = new File(path);
            if (!dir.exists())
                dir.mkdir();
            File file = new File(dir, filename + ".pdf");
            FileOutputStream fOut = new FileOutputStream(file);
            PdfWriter.getInstance(document, fOut);
            document.open();
            document.add(paragraph);
        } catch (DocumentException de) {
            Toast.makeText(this, de.getMessage(), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            document.close();
        }
        Toast.makeText(this, "File Saved successfully", Toast.LENGTH_LONG).show();
        /*Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri data = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileProvider",
                new File(path+"/"+filename+".pdf"));
        intent.setDataAndType(data, "application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        try {
            startActivity(Intent.createChooser(intent, "Choose One Application To Open File"));
        } catch (Exception e) {
            Toast.makeText(this, "No PdfViewer To Open File", Toast.LENGTH_LONG).show();
        }*/
        startActivity(new Intent(EditActivity.this,SavedFilesActivity.class));
    }


}
