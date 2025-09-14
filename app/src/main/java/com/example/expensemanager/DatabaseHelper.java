package com.expensemanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "finance.db";
    private static final int DATABASE_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, amount REAL, note TEXT, date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS transactions");
        onCreate(db);
    }

    public void insertTransaction(String type, double amount, String note) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("amount", amount);
        values.put("note", note);
        db.insert("transactions", null, values);
    }

    public List<Transaction> getAllTransactions(String type) {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM transactions WHERE type=?", new String[]{type});
        if (cursor.moveToFirst()) {
            do {
                double amount = cursor.getDouble(cursor.getColumnIndex("amount"));
                String note = cursor.getString(cursor.getColumnIndex("note"));
                String date = cursor.getString(cursor.getColumnIndex("date"));
                list.add(new Transaction(type, amount, note, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public double getTotalByType(String type) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) as total FROM transactions WHERE type=?", new String[]{type});
        double total = 0;
        if (cursor.moveToFirst()) { total = cursor.getDouble(cursor.getColumnIndex("total")); }
        cursor.close();
        return total;
    }

    public ArrayList<BarEntry> getMonthlyBarEntries() {
        // Placeholder: implement monthly grouping and return chart entries.
        return new ArrayList<>();
    }
}
