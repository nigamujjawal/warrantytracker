package com.uj.warrantytrackerapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class ViewSavedWarrantiesActivity extends AppCompatActivity {

    private LinearLayout listContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_saved_warranty);

        listContainer = findViewById(R.id.listContainer);

        // Get logged-in user's email from session
        SharedPreferences sessionPrefs = getSharedPreferences("user_session", MODE_PRIVATE);
        String loggedInEmail = sessionPrefs.getString("logged_in_email", null);

        if (loggedInEmail == null) {
            TextView errorText = new TextView(this);
            errorText.setText("User not logged in.");
            errorText.setTextSize(18f);
            listContainer.addView(errorText);
            return;
        }

        // Load saved warranties specific to the user
        SharedPreferences prefs = getSharedPreferences("warranties", MODE_PRIVATE);
        String userKey = "warranty_list_" + loggedInEmail;
        String json = prefs.getString(userKey, "[]");

        Gson gson = new Gson();
        Type type = new TypeToken<List<WarrantyItem>>() {}.getType();
        List<WarrantyItem> list = gson.fromJson(json, type);

        if (list == null || list.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText("No warranties saved yet.");
            emptyText.setTextSize(18f);
            listContainer.addView(emptyText);
        } else {
            for (WarrantyItem item : list) {
                TextView tv = new TextView(this);
                tv.setText(
                        "📦 Product: " + item.getProductName() + "\n" +
                                "🛒 Purchase: " + item.getPurchaseDate() + "\n" +
                                "⏳ Duration: " + item.getWarrantyDuration() + " months\n" +
                                "📅 Expiry: " + item.getExpiryDate()
                );
                tv.setPadding(20, 20, 20, 20);
                tv.setTextSize(16f);
                tv.setBackgroundColor(0xFFE0E0E0); // light gray background
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 16, 0, 0);
                tv.setLayoutParams(params);
                listContainer.addView(tv);
            }
        }
    }
}
