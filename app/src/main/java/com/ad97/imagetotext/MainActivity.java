package com.ad97.imagetotext;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.text.TextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;

import static android.Manifest.permission.CAMERA;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.text.Text;
import com.google.android.gms.vision.text.TextBlock;

public class MainActivity extends AppCompatActivity {

    /**
     * TextView to show the scanned text from the image
     */
    private TextView scannedText;
    /**
     * Selected of Clicked image Uri
     */
    private Uri imageUri;
    /**
     * permissionFlag is for checking runtime permission is enabled or not
     * angleFlag is for scanning image at all angles if it is true
     */
    private Boolean permissionFlag = true, angleFlag = false;
    /**
     * detector is for the Text Recognition from the image
     */
    private TextRecognizer detector;
    /**
     * lines is for the lines scanned from the image
     */
    private StringBuilder lines;
    /**
     * spaceForText is scrollView for scrolling scanned text
     */
    private ScrollView spaceForText;
    /**
     * more button is used when the image is scanned using angle detection to show more then one scanned text
     */
    private Button more;
    /**
     * edit button is used to edit or save the scanned text
     */
    private FloatingActionButton edit;
    /**
     * selected image is used to show the image which is scanned by the OCR
     */
    private FloatingActionButton selectedImage;
    /**
     * list to store text block list of the image scanned by OCR
     */
    private ArrayList<SparseArray<TextBlock>> textBlockList;
    private int circular;
    /**
     * request codes for different purposes
     */
    private final int CLICK_REQUEST_CODE = 1, GALLERY_REQUEST_CODE = 2, PERMISSION_REQUEST_CODE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (!permissionsIsEnabledOrNot())
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{WRITE_EXTERNAL_STORAGE, READ_EXTERNAL_STORAGE, CAMERA}, PERMISSION_REQUEST_CODE);
        scannedText = findViewById(R.id.scannedText);
        detector = new TextRecognizer.Builder(getApplicationContext()).build();
        spaceForText = findViewById(R.id.spaceForText);
        more = findViewById(R.id.more);
        more.setVisibility(View.GONE);
        edit = findViewById(R.id.edit);
        edit.setVisibility(View.GONE);
        selectedImage = findViewById(R.id.selectedImage);
        selectedImage.setVisibility(View.GONE);
        findViewById(R.id.camera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permissionFlag)
                    clickImage();
            }
        });

        findViewById(R.id.gallery).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (permissionFlag)
                    selectImage();
            }
        });

        findViewById(R.id.files).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SavedFilesActivity.class));
            }
        });

        edit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, EditActivity.class).putExtra("scanned_text", lines.toString()));

            }
        });

        more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                circular = circular == textBlockList.size() - 1 ? 0 : circular + 1;
                setTextInBlock(textBlockList.get(circular));
            }
        });

        selectedImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final Dialog dialog = new Dialog(MainActivity.this);
                dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                dialog.setContentView(R.layout.layout_image);
                dialog.setCancelable(true);
                ImageView image = dialog.findViewById(R.id.image);
                try {
                    Bitmap bitmap = decodeBitmapUri(imageUri);
                    image.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                dialog.show();
            }
        });
    }

    /**
     * method to select image from gallery for scanning
     */
    private void selectImage() {
        imageUri = null;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select image"), GALLERY_REQUEST_CODE);
    }

    /**
     * method to click image using phone camera for scanning
     */
    private void clickImage() {
        imageUri = null;
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File imageFile = new File(Environment.getExternalStorageDirectory().toString(), "image" + String.valueOf(System.currentTimeMillis() + ".jpg"));
        imageUri = FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileProvider", imageFile);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraIntent.putExtra("return-data", true);
        startActivityForResult(cameraIntent, CLICK_REQUEST_CODE);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CLICK_REQUEST_CODE && resultCode == RESULT_OK) {
            CropImage.activity(imageUri).start(this);
        } else if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            CropImage.activity(data.getData()).start(this);
        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            imageUri = result.getUri();
            edit.setVisibility(View.GONE);
            selectedImage.setVisibility(View.GONE);
            spaceForText.setVisibility(View.INVISIBLE);
            more.setVisibility(View.GONE);
            launchMediaScanIntent();
            alertForAngleDetection();
        }
    }

    /**
     * alert asking for angular detection or simple detection
     */
    private void alertForAngleDetection() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Tilt Image Detection");

        builder.setMessage("If your Image is tilted then Click 'Angle Detection' to get Better Result");
        builder.setPositiveButton("Angle Detection", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                angleFlag = true;
                new Detect().execute(imageUri);
            }
        });
        builder.setNegativeButton("Simple Detection", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                angleFlag = false;
                new Detect().execute(imageUri);
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    /**
     * AsyncTask for scanning image using OCR to extract text on UI Thread
     */
    @SuppressLint("StaticFieldLeak")
    class Detect extends AsyncTask<Uri, Integer, Boolean> {

        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute() {
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setIndeterminate(false);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.setTitle("Scanning...");
            if (angleFlag)
                progressDialog.setMessage("At all angles for better result\nPlease wait .. ");
            else
                progressDialog.setMessage("Please wait .. ");
            progressDialog.setMax(100);
            progressDialog.setCancelable(false);
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressDialog.setProgress(values[0]);
        }

        @Override
        protected Boolean doInBackground(Uri... uris) {

            Frame frame;
            textBlockList = new ArrayList<>();
            try {
                Bitmap originalBitmap = decodeBitmapUri(uris[0]);
                int percentage = angleFlag ? 0 : 50;
                publishProgress(percentage);
                if (detector.isOperational() && originalBitmap != null) {
                    frame = new Frame.Builder().setBitmap(originalBitmap).build();
                    if (detector.detect(frame).size() != 0)
                        textBlockList.add(detector.detect(frame));
                    if (angleFlag) {
                        Matrix matrix = new Matrix();
                        int degree;
                        for (degree = 15; degree <= 90; degree += 15) {
                            matrix.postRotate(degree);
                            Bitmap bitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                            frame = new Frame.Builder().setBitmap(bitmap).build();
                            if (detector.detect(frame).size() != 0)
                                textBlockList.add(detector.detect(frame));
                            percentage += 8;
                            onProgressUpdate(percentage);
                        }
                        for (degree = -15; degree >= -90; degree -= 15) {
                            matrix.postRotate(degree);
                            Bitmap bitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                            frame = new Frame.Builder().setBitmap(bitmap).build();
                            if (detector.detect(frame).size() != 0)
                                textBlockList.add(detector.detect(frame));
                            percentage += 8;
                            onProgressUpdate(percentage);
                        }
                    }
                    percentage = 100;
                    onProgressUpdate(percentage);
                    return true;
                } else
                    return false;

            } catch (FileNotFoundException e) {
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {

            if (result)
                setExtractedText();
            else
                Toast.makeText(MainActivity.this, "Failed to load Image", Toast.LENGTH_SHORT).show();
            progressDialog.dismiss();
        }
    }

    /**
     * method used to set extracted text from the scanned image
     */
    private void setExtractedText() {

        spaceForText.setVisibility(View.VISIBLE);
        switch (textBlockList.size()) {
            case 0:
                String SCANNED_FAILED = "Scan Failed: Found nothing to scan";
                scannedText.setText(SCANNED_FAILED);
                break;
            default:
                more.setVisibility(View.VISIBLE);
                circular = 0;
            case 1:
                edit.setVisibility(View.VISIBLE);
                selectedImage.setVisibility(View.VISIBLE);
                setTextInBlock(textBlockList.get(0));
        }
        spaceForText.setBackgroundResource(R.drawable.text_background);
    }

    /**
     * method for set text using blocks scanned by OCR in lines
     *
     * @param textBlocks blocks scanned by OCR
     */
    private void setTextInBlock(SparseArray<TextBlock> textBlocks) {
        lines = new StringBuilder("");
        for (int index = 0; index < textBlocks.size(); index++) {
            //extract scanned text blocks here
            TextBlock tBlock = textBlocks.valueAt(index);
            for (Text line : tBlock.getComponents()) {
                //extract scanned text lines here
                lines.append(line.getValue());
            }
            lines.append("\n");
        }
        scannedText.setText(lines);
    }

    /**
     * method for decode of bitmap from imageUri for detecting text
     *
     * @param uri imageUri
     * @return decoded Bitmap from imageUri
     * @throws FileNotFoundException if file not found this exception is throws at runtime
     */
    private Bitmap decodeBitmapUri(Uri uri) throws FileNotFoundException {

        int targetW = 600;
        int targetH = 600;
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(this.getContentResolver().openInputStream(uri), null, bmOptions);

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH / targetH);
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        return BitmapFactory.decodeStream(this.getContentResolver().openInputStream(uri), null, bmOptions);
    }

    private void launchMediaScanIntent() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(imageUri);
        this.sendBroadcast(mediaScanIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                boolean flag = false;
                for (int item : grantResults) {
                    if (item != PackageManager.PERMISSION_GRANTED) {
                        flag = true;
                        break;
                    }
                }
                if (flag) {
                    Toast.makeText(MainActivity.this, "Permission Denied", Toast.LENGTH_SHORT).show();
                    permissionFlag = false;
                }

            }
        }
    }

    private boolean permissionsIsEnabledOrNot() {
        return checkPermission(WRITE_EXTERNAL_STORAGE) &&
                checkPermission(READ_EXTERNAL_STORAGE) &&
                checkPermission(CAMERA);
    }

    private boolean checkPermission(String name) {
        return ContextCompat.checkSelfPermission(getApplicationContext(), name) == PackageManager.PERMISSION_GRANTED;
    }


     /*private void detectDifferentFrames() {

        Frame frame;
        textBlockList = new ArrayList<>();
        try {
            Bitmap originalBitmap = decodeBitmapUri(this, imageUri);
            if (detector.isOperational() && originalBitmap != null) {
                frame = new Frame.Builder().setBitmap(originalBitmap).build();
                if (detector.detect(frame).size() != 0)
                    textBlockList.add(detector.detect(frame));
                Matrix matrix = new Matrix();
                int degree;
                for (degree = 15; degree <= 90; degree += 15) {
                    matrix.postRotate(degree);
                    Bitmap bitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                    frame = new Frame.Builder().setBitmap(bitmap).build();
                    if (detector.detect(frame).size() != 0)
                        textBlockList.add(detector.detect(frame));
                }
                for (degree = -15; degree >= -90; degree -= 15) {
                    matrix.postRotate(degree);
                    Bitmap bitmap = Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(), originalBitmap.getHeight(), matrix, true);
                    frame = new Frame.Builder().setBitmap(bitmap).build();
                    if (detector.detect(frame).size() != 0)
                        textBlockList.add(detector.detect(frame));
                }
                setExtractedText();
            } else {
                String DETECTOR_NOT_SETUP = "Could not set up the detector!";
                scannedText.setText(DETECTOR_NOT_SETUP);
            }
            imageUri = null;
        } catch (FileNotFoundException e) {
            Toast.makeText(this, "Failed to load Image", Toast.LENGTH_SHORT).show();
        }

    }
*/
}
