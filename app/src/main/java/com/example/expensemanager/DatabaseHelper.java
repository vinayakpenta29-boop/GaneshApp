package com.expensemanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "finance.db";
    private static final int DATABASE_VERSION = 2; // Bumped version for schema change

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE transactions (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT, amount REAL, note TEXT, month TEXT, year TEXT, date TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS transactions");
        onCreate(db);
    }

    public void insertTransaction(String type, double amount, String note, String month, String year) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("amount", amount);
        values.put("note", note != null ? note : "");
        values.put("month", month);
        values.put("year", year);
        db.insert("transactions", null, values);
    }

    // Update getAllTransactions and relevant queries as needed for month/year if required

    // Returns grouped monthly totals for income and expense bars, with matching months/year
    public void getGroupedMonthlyEntries(ArrayList<BarEntry> incomeEntries, ArrayList<BarEntry> expenseEntries, ArrayList<String> monthLabels, String year) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT month, SUM(CASE WHEN type='income' THEN amount ELSE 0 END) AS income, " +
            "SUM(CASE WHEN type='expense' THEN amount ELSE 0 END) AS expense " +
            "FROM transactions WHERE year=? GROUP BY month ORDER BY CAST(month AS INTEGER)", 
            new String[]{year});
        int i = 0;
        while (cursor.moveToNext()) {
            String month = cursor.getString(cursor.getColumnIndex("month"));
            float income = (float) cursor.getDouble(cursor.getColumnIndex("income"));
            float expense = (float) cursor.getDouble(cursor.getColumnIndex("expense"));
            incomeEntries.add(new BarEntry(i, income));
            expenseEntries.add(new BarEntry(i, expense));
            monthLabels.add(month);
            i++;
        }
        cursor.close();
        // If empty, add dummy values for chart stability
        if (incomeEntries.isEmpty() && expenseEntries.isEmpty()) {
            incomeEntries.add(new BarEntry(0, 0));
            expenseEntries.add(new BarEntry(0, 0));
            monthLabels.add("NA");
        }
    }
}
