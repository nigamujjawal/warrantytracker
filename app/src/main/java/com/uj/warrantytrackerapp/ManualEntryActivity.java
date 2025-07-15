package com.uj.warrantytrackerapp;

import android.app.DatePickerDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.*;

public class ManualEntryActivity extends AppCompatActivity {

    EditText productName, purchaseDate, warrantyDuration, expiryDate;
    Button saveButton;
    Calendar selectedCalendar = Calendar.getInstance();
    Calendar expiryCalendar = Calendar.getInstance();

    private String loggedInEmail;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_entry);

        // Fetch the logged-in user's email
        SharedPreferences sessionPrefs = getSharedPreferences("user_session", MODE_PRIVATE);
        loggedInEmail = sessionPrefs.getString("logged_in_email", null);

        if (loggedInEmail == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        productName = findViewById(R.id.productName);
        purchaseDate = findViewById(R.id.purchaseDate);
        warrantyDuration = findViewById(R.id.warrantyDuration);
        expiryDate = findViewById(R.id.expiryDate);
        saveButton = findViewById(R.id.saveButton);

        purchaseDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedCalendar.set(year, month, dayOfMonth);
                String formattedDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(selectedCalendar.getTime());
                purchaseDate.setText(formattedDate);
                calculateExpiryDate();
            }, selectedCalendar.get(Calendar.YEAR),
                    selectedCalendar.get(Calendar.MONTH),
                    selectedCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        expiryDate.setOnClickListener(v -> {
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                expiryCalendar.set(year, month, dayOfMonth);
                String formatted = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(expiryCalendar.getTime());
                expiryDate.setText(formatted);
            }, expiryCalendar.get(Calendar.YEAR),
                    expiryCalendar.get(Calendar.MONTH),
                    expiryCalendar.get(Calendar.DAY_OF_MONTH)).show();
        });

        warrantyDuration.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                calculateExpiryDate();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        saveButton.setOnClickListener(v -> {
            String name = productName.getText().toString().trim();
            String purDate = purchaseDate.getText().toString().trim();
            String durationStr = warrantyDuration.getText().toString().trim();
            String expDate = expiryDate.getText().toString().trim();

            if (name.isEmpty() || purDate.isEmpty() || durationStr.isEmpty() || expDate.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            WarrantyItem item = new WarrantyItem(name, purDate, durationStr, expDate);

            SharedPreferences prefs = getSharedPreferences("warranties", MODE_PRIVATE);
            Gson gson = new Gson();
            Type type = new TypeToken<List<WarrantyItem>>() {}.getType();

            // Use a user-specific key
            String userKey = "warranty_list_" + loggedInEmail;
            List<WarrantyItem> list = gson.fromJson(prefs.getString(userKey, "[]"), type);
            if (list == null) list = new ArrayList<>();

            list.add(item);
            prefs.edit().putString(userKey, gson.toJson(list)).apply();

            Toast.makeText(this, "Saved successfully!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void calculateExpiryDate() {
        String durationStr = warrantyDuration.getText().toString().trim();
        if (!durationStr.isEmpty() && !purchaseDate.getText().toString().isEmpty()) {
            try {
                int months = Integer.parseInt(durationStr);
                Calendar expiryCal = (Calendar) selectedCalendar.clone();
                expiryCal.add(Calendar.MONTH, months);

                String expDateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        .format(expiryCal.getTime());
                expiryDate.setText(expDateStr);
            } catch (Exception e) {
                expiryDate.setText("");
            }
        }
    }
}
