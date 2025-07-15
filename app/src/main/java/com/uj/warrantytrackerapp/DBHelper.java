package com.uj.warrantytrackerapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "UserDB.db";
    private static final int DATABASE_VERSION = 2;

    // User table
    private static final String TABLE_USERS = "users";
    private static final String COL_NAME = "name";
    private static final String COL_EMAIL = "email";
    private static final String COL_PASSWORD = "password";

    // Warranty table
    private static final String TABLE_WARRANTIES = "warranties";
    private static final String COL_ID = "id";
    private static final String COL_PRODUCT_NAME = "productName";
    private static final String COL_PURCHASE_DATE = "purchaseDate";
    private static final String COL_DURATION = "warrantyDuration";
    private static final String COL_EXPIRY_DATE = "expiryDate";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create user table
        String createUserTable = "CREATE TABLE " + TABLE_USERS + " (" +
                COL_EMAIL + " TEXT PRIMARY KEY, " +
                COL_NAME + " TEXT, " +
                COL_PASSWORD + " TEXT)";
        db.execSQL(createUserTable);

        // Create warranty table
        String createWarrantyTable = "CREATE TABLE " + TABLE_WARRANTIES + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PRODUCT_NAME + " TEXT, " +
                COL_PURCHASE_DATE + " TEXT, " +
                COL_DURATION + " TEXT, " +
                COL_EXPIRY_DATE + " TEXT)";
        db.execSQL(createWarrantyTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WARRANTIES);
        onCreate(db);
    }

    // =========== USER TABLE METHODS ===========
    public boolean registerUser(String name, String email, String password) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NAME, name);
        values.put(COL_EMAIL, email);
        values.put(COL_PASSWORD, password);

        long result = db.insert(TABLE_USERS, null, values);
        return result != -1;
    }

    public boolean checkUser(String email, String password) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_USERS +
                " WHERE email=? AND password=?", new String[]{email, password});
        return cursor.getCount() > 0;
    }

    public Cursor getAllUsers() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_USERS, null);
    }

    // =========== WARRANTY TABLE METHODS ===========
    public boolean insertWarranty(String productName, String purchaseDate, String duration, String expiryDate) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_PRODUCT_NAME, productName);
        values.put(COL_PURCHASE_DATE, purchaseDate);
        values.put(COL_DURATION, duration);
        values.put(COL_EXPIRY_DATE, expiryDate);

        long result = db.insert(TABLE_WARRANTIES, null, values);
        return result != -1;
    }

    public Cursor getAllWarranties() {
        SQLiteDatabase db = this.getReadableDatabase();
        return db.rawQuery("SELECT * FROM " + TABLE_WARRANTIES, null);
    }
}
