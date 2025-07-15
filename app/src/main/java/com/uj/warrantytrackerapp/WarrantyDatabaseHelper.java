package com.uj.warrantytrackerapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class WarrantyDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "warranty_db";
    private static final int DB_VERSION = 1;

    public WarrantyDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE warranty_items (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "productName TEXT, " +
                "purchaseDate TEXT, " +
                "warrantyDuration TEXT, " +
                "expiryDate TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS warranty_items");
        onCreate(db);
    }

    public void insertWarranty(WarrantyItem item) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("productName", item.getProductName());
        values.put("purchaseDate", item.getPurchaseDate());
        values.put("warrantyDuration", item.getWarrantyDuration());
        values.put("expiryDate", item.getExpiryDate());
        db.insert("warranty_items", null, values);
        db.close();
    }

    public List<WarrantyItem> getAllWarranties() {
        List<WarrantyItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM warranty_items", null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow("productName"));
                String purchase = cursor.getString(cursor.getColumnIndexOrThrow("purchaseDate"));
                String duration = cursor.getString(cursor.getColumnIndexOrThrow("warrantyDuration"));
                String expiry = cursor.getString(cursor.getColumnIndexOrThrow("expiryDate"));
                list.add(new WarrantyItem(name, purchase, duration, expiry));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return list;
    }
}
