package com.uj.warrantytrackerapp;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;

public class OCRCaptureActivity extends AppCompatActivity {

    private ImageView ocrImagePreview;
    private ProgressBar ocrProgressBar;
    private String currentPhotoPath;

    // Launch camera
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    File file = new File(currentPhotoPath);
                    Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                    ocrImagePreview.setVisibility(View.VISIBLE);
                    ocrImagePreview.setImageBitmap(bitmap);
                    runOCR(bitmap);
                }
            });

    // Launch gallery picker
    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        ocrImagePreview.setVisibility(View.VISIBLE);
                        ocrImagePreview.setImageBitmap(bitmap);
                        runOCR(bitmap);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Image loading failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocrcapture);

        ocrImagePreview = findViewById(R.id.ocrImagePreview);
        ocrProgressBar = findViewById(R.id.ocrProgressBar);

        requestAllPermissions();
        showImagePickerOptions();
    }

    private void requestAllPermissions() {
        List<String> permissions = new ArrayList<>();
        permissions.add(Manifest.permission.CAMERA);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }

        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, permissions.toArray(new String[0]), 1);
                return;
            }
        }
    }

    private void showImagePickerOptions() {
        String[] options = {"Camera", "Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Image Source");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) openCamera();
            else openGallery();
        });
        builder.show();
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                ex.printStackTrace();
                return;
            }

            Uri photoURI = FileProvider.getUriForFile(this,
                    getPackageName() + ".provider", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void runOCR(Bitmap bitmap) {
        ocrProgressBar.setVisibility(View.VISIBLE);

        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener(result -> {
                    ocrProgressBar.setVisibility(View.GONE);
                    String extractedText = result.getText();
                    List<String> dates = extractDatesFromText(extractedText);
                    if (!dates.isEmpty()) {
                        String selectedDate = dates.get(0);
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("purchaseDate", selectedDate);  // ✅ THIS IS CRUCIAL
                        setResult(Activity.RESULT_OK, resultIntent);          // ✅ Don't forget this
                        finish();                                             // ✅ Finish the activity
                    } else {
                        Toast.makeText(this, "No date found", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    ocrProgressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "OCR Failed", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                });
    }

    private List<String> extractDatesFromText(String text) {
        List<String> dateList = new ArrayList<>();
        String[] patterns = {
                "\\b\\d{4}[-/]\\d{2}[-/]\\d{2}\\b",     // 2023-07-13 or 2023/07/13
                "\\b\\d{2}[-/]\\d{2}[-/]\\d{4}\\b",     // 13-07-2023 or 13/07/2023
                "\\b\\d{2}[.]\\d{2}[.]\\d{4}\\b"        // 13.07.2023
        };

        for (String pattern : patterns) {
            Matcher matcher = Pattern.compile(pattern).matcher(text);
            while (matcher.find()) {
                String date = matcher.group().replace('/', '-').replace('.', '-');
                dateList.add(date);
            }
        }
        return dateList;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        for (int res : grantResults) {
            if (res != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}

