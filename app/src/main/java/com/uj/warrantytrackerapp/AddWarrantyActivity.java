package com.uj.warrantytrackerapp;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

public class AddWarrantyActivity extends AppCompatActivity {

    private EditText editProductName, editPurchaseDate, editDuration;
    private Button btnOcrScan, manualEntryButton, saveButton, viewSavedButton;
    private LinearLayout formLayout;

    private String loggedInEmail;

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

        // Fetch the logged-in user's email
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
        btnOcrScan = findViewById(R.id.uploadImageButton);
        manualEntryButton = findViewById(R.id.manualEntryButton);
        saveButton = findViewById(R.id.saveButton);
        viewSavedButton = findViewById(R.id.viewSavedButton);
        formLayout = findViewById(R.id.formLayout);

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

        formLayout.setVisibility(View.GONE);
        editProductName.setText("");
        editPurchaseDate.setText("");
        editDuration.setText("");
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
}
