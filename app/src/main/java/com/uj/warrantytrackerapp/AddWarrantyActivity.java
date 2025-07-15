package com.uj.warrantytrackerapp;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

import android.location.Location;

public class AddWarrantyActivity extends AppCompatActivity {

    private EditText editProductName, editPurchaseDate, editDuration, editCompanyName;
    private Button btnOcrScan, manualEntryButton, saveButton, viewSavedButton, testNotifyBtn, btnFindServiceCenter;
    private LinearLayout formLayout;
    private String loggedInEmail;
    private FusedLocationProviderClient fusedLocationClient;

    private final ActivityResultLauncher<Intent> ocrLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    String extractedDate = result.getData().getStringExtra("purchaseDate");
                    if (extractedDate != null && !extractedDate.isEmpty()) {
                        formLayout.setVisibility(View.VISIBLE);
                        editPurchaseDate.setText(extractedDate);
                    } else {
                        Toast.makeText(this, "No date found", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_warranty);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "warranty_channel",
                    "Warranty Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }

        SharedPreferences sessionPrefs = getSharedPreferences("user_session", MODE_PRIVATE);
        loggedInEmail = sessionPrefs.getString("logged_in_email", null);

        if (loggedInEmail == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        editProductName = findViewById(R.id.editProductName);
        editPurchaseDate = findViewById(R.id.editPurchaseDate);
        editDuration = findViewById(R.id.editDuration);
        editCompanyName = findViewById(R.id.editCompanyName);
        btnOcrScan = findViewById(R.id.uploadImageButton);
        manualEntryButton = findViewById(R.id.manualEntryButton);
        saveButton = findViewById(R.id.saveButton);
        viewSavedButton = findViewById(R.id.viewSavedButton);
        testNotifyBtn = findViewById(R.id.testNotifyBtn);
        btnFindServiceCenter = findViewById(R.id.btnFindServiceCenter);
        formLayout = findViewById(R.id.formLayout);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        editPurchaseDate.setOnClickListener(v -> showDatePicker());

        btnOcrScan.setOnClickListener(v -> {
            Intent intent = new Intent(AddWarrantyActivity.this, OCRCaptureActivity.class);
            ocrLauncher.launch(intent);
        });

        manualEntryButton.setOnClickListener(v -> {
            Intent intent = new Intent(AddWarrantyActivity.this, ManualEntryActivity.class);
            startActivity(intent);
        });

        viewSavedButton.setOnClickListener(v -> {
            Intent intent = new Intent(AddWarrantyActivity.this, ViewSavedWarrantiesActivity.class);
            startActivity(intent);
        });

        saveButton.setOnClickListener(v -> saveWarranty());

        testNotifyBtn.setOnClickListener(v -> {
            Intent testIntent = new Intent(this, NotificationReceiver.class);
            testIntent.putExtra("productName", "Demo Product");
            sendBroadcast(testIntent);
        });

        btnFindServiceCenter.setOnClickListener(v -> findNearestServiceCenter());
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    String formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
                    editPurchaseDate.setText(formattedDate);
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void saveWarranty() {
        String productName = editProductName.getText().toString().trim();
        String purchaseDate = editPurchaseDate.getText().toString().trim();
        String durationStr = editDuration.getText().toString().trim();

        if (productName.isEmpty() || purchaseDate.isEmpty() || durationStr.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        int duration;
        try {
            duration = Integer.parseInt(durationStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid duration format", Toast.LENGTH_SHORT).show();
            return;
        }

        String expiryDate = calculateExpiryDate(purchaseDate, duration);
        WarrantyItem newItem = new WarrantyItem(productName, purchaseDate, durationStr, expiryDate);

        SharedPreferences prefs = getSharedPreferences("warranties", MODE_PRIVATE);
        Gson gson = new Gson();
        Type type = new TypeToken<List<WarrantyItem>>() {}.getType();

        String userKey = "warranty_list_" + loggedInEmail;
        List<WarrantyItem> list = gson.fromJson(prefs.getString(userKey, "[]"), type);
        if (list == null) list = new ArrayList<>();

        list.add(newItem);
        prefs.edit().putString(userKey, gson.toJson(list)).apply();

        Toast.makeText(this, "Warranty Saved!", Toast.LENGTH_SHORT).show();

        scheduleNotification(productName, expiryDate);

        formLayout.setVisibility(View.GONE);
        editProductName.setText("");
        editPurchaseDate.setText("");
        editDuration.setText("");
        editCompanyName.setText("");
    }

    private String calculateExpiryDate(String purchaseDate, int months) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(sdf.parse(purchaseDate));
            calendar.add(Calendar.MONTH, months);
            return sdf.format(calendar.getTime());
        } catch (Exception e) {
            return "Invalid Date";
        }
    }

    private void scheduleNotification(String productName, String expiryDateStr) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date expiryDate = sdf.parse(expiryDateStr);
            if (expiryDate == null) return;

            Calendar notifyTime = Calendar.getInstance();
            notifyTime.setTime(expiryDate);
            notifyTime.add(Calendar.DAY_OF_YEAR, -3);

            long delayMillis = notifyTime.getTimeInMillis() - System.currentTimeMillis();
            if (delayMillis < 0) return;

            Intent intent = new Intent(this, NotificationReceiver.class);
            intent.putExtra("productName", productName);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.set(AlarmManager.RTC_WAKEUP,
                        System.currentTimeMillis() + delayMillis,
                        pendingIntent);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void findNearestServiceCenter() {
        String companyName = editCompanyName.getText().toString().trim();
        if (companyName.isEmpty()) {
            Toast.makeText(this, "Enter company name first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1001);
            return;
        }

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                // Correct and compatible URI format
                String searchQuery = Uri.encode(companyName + " service center near me");
                Uri gmmIntentUri = Uri.parse("geo:" + latitude + "," + longitude + "?q=" + searchQuery);

                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");

                try {
                    startActivity(mapIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Unable to launch Google Maps.", Toast.LENGTH_LONG).show();
                }

            } else {
                Toast.makeText(this, "Location not found. Try again later.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to get location: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                findNearestServiceCenter();
            } else {
                Toast.makeText(this, "Location permission is required to find service centers", Toast.LENGTH_LONG).show();
            }
        }
    }
}
