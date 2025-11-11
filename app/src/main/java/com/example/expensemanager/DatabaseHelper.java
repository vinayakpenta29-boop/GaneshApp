package com.expensemanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import com.github.mikephil.charting.data.BarEntry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "finance.db";
    private static final int DATABASE_VERSION = 2;

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

    public ArrayList<Transaction> getAllTransactions(String type) {
        ArrayList<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM transactions WHERE type=?", new String[]{type});
        if (cursor.moveToFirst()) {
            do {
                double amount = cursor.getDouble(cursor.getColumnIndex("amount"));
                String note = cursor.getString(cursor.getColumnIndex("note"));
                String month = cursor.getString(cursor.getColumnIndex("month"));
                String year = cursor.getString(cursor.getColumnIndex("year"));
                String date = cursor.getString(cursor.getColumnIndex("date"));
                list.add(new Transaction(type, amount, note, month, year, date));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    public double getTotalByType(String type) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT SUM(amount) as total FROM transactions WHERE type=?", new String[]{type});
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(cursor.getColumnIndex("total"));
        }
        cursor.close();
        return total;
    }

    public List<String> getAllYears() {
        List<String> years = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT DISTINCT year FROM transactions ORDER BY year ASC", null);
        while (cursor.moveToNext()) {
            years.add(cursor.getString(0));
        }
        cursor.close();
        return years;
    }

    // For summary and graph aggregations
    public void getGroupedMonthlyValues(float[] incomeByMonth, float[] expenseByMonth, String year) {
        for (int i = 0; i < 12; i++) {
            incomeByMonth[i] = 0f;
            expenseByMonth[i] = 0f;
        }
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT month, " +
            "SUM(CASE WHEN type='income' THEN amount ELSE 0 END) AS income, " +
            "SUM(CASE WHEN type='expense' THEN amount ELSE 0 END) AS expense " +
            "FROM transactions WHERE year=? GROUP BY month ORDER BY CAST(month AS INTEGER)",
            new String[]{year});

        while (cursor.moveToNext()) {
            String monthStr = cursor.getString(cursor.getColumnIndex("month"));
            int monthIdx = -1;
            try {
                monthIdx = Integer.parseInt(monthStr) - 1;
            } catch (Exception ignored) {}

            if (monthIdx >= 0 && monthIdx < 12) {
                incomeByMonth[monthIdx] = (float) cursor.getDouble(cursor.getColumnIndex("income"));
                expenseByMonth[monthIdx] = (float) cursor.getDouble(cursor.getColumnIndex("expense"));
            }
        }
        cursor.close();
    }

    // Optionally, legacy method for grouped entries
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
        if (incomeEntries.isEmpty() && expenseEntries.isEmpty()) {
            incomeEntries.add(new BarEntry(0, 0));
            expenseEntries.add(new BarEntry(0, 0));
            monthLabels.add("NA");
        }
    }

    public Map<String, List<Transaction>> getTransactionsByTypeAndYearGroupedByMonth(String type, String year) {
        Map<String, List<Transaction>> map = new LinkedHashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
            "SELECT * FROM transactions WHERE type=? AND year=? ORDER BY CAST(month AS INTEGER), date DESC",
            new String[]{type, year});
        while (cursor.moveToNext()) {
            double amount = cursor.getDouble(cursor.getColumnIndex("amount"));
            String note = cursor.getString(cursor.getColumnIndex("note"));
            String month = cursor.getString(cursor.getColumnIndex("month"));
            String txnYear = cursor.getString(cursor.getColumnIndex("year"));
            String date = cursor.getString(cursor.getColumnIndex("date"));
            Transaction txn = new Transaction(type, amount, note, month, txnYear, date);
            if (!map.containsKey(month)) {
                map.put(month, new ArrayList<>());
            }
            map.get(month).add(txn);
        }
        cursor.close();
        return map;
    }

    // --- Add this method for full data wipe/reset ---
    public void clearAllData() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("transactions", null, null);
    }
}
