package com.expensemanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.Calendar;

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
        values.put("note", note != null ? note : "");
        db.insert("transactions", null, values);
    }

    public ArrayList<Transaction> getAllTransactions(String type) {
        ArrayList<Transaction> list = new ArrayList<>();
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

    // Simple example implementation — sum amounts by month (January=0..December=11)
    public ArrayList<BarEntry> getMonthlyBarEntries() {
        ArrayList<BarEntry> entries = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        // Query to group by month (SQLite strftime '%m' for month)
        Cursor cursor = db.rawQuery(
            "SELECT strftime('%m', date) as month, SUM(amount) as total FROM transactions GROUP BY month ORDER BY month ASC", null);
        if (cursor.moveToFirst()) {
            do {
                int month = Integer.parseInt(cursor.getString(cursor.getColumnIndex("month"))) - 1; // zero-based for BarEntry
                float total = (float) cursor.getDouble(cursor.getColumnIndex("total"));
                entries.add(new BarEntry(month, total));
            } while (cursor.moveToNext());
        }
        cursor.close();

        // Return dummy data if empty to avoid empty graph
        if(entries.isEmpty()) {
            entries.add(new BarEntry(0, 0f));
        }
        return entries;
    }
}
