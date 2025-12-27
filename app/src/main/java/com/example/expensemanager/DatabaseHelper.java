package com.expensemanager;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.ContentValues;
import android.database.Cursor;

import com.github.mikephil.charting.data.BarEntry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.Date;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "finance.db";
    // Bumped to 4 because schema changes (added source_type)
    private static final int DATABASE_VERSION = 4;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "CREATE TABLE transactions (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "type TEXT, " +
                        "amount REAL, " +
                        "note TEXT, " +
                        "month TEXT, " +
                        "year TEXT, " +
                        "category TEXT, " +
                        "source_type TEXT, " +    // which radio (SALARY/COMMISSION/OTHER)
                        "date TEXT)"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Simple migration: if coming from version < 4, add source_type column
        if (oldVersion < 4) {
            try {
                db.execSQL("ALTER TABLE transactions ADD COLUMN source_type TEXT");
            } catch (Exception ignored) {
                // If column already exists, ignore
            }
        }
    }

    // New main insert: includes category and sourceType (radio)
    public void insertTransaction(String type,
                                  double amount,
                                  String note,
                                  String month,
                                  String year,
                                  String category,
                                  String sourceType) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("type", type);
        values.put("amount", amount);
        values.put("note", note != null ? note : "");
        values.put("month", month);
        values.put("year", year);
        values.put("category", category);
        values.put("source_type", sourceType); // may be null

        // Use device time in IST for 'date'
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Kolkata")); // IST
        String now = sdf.format(new Date());
        values.put("date", now);

        db.insert("transactions", null, values);
    }

    // Existing 6‑arg overload now calls the main insert with null sourceType
    public void insertTransaction(String type, double amount, String note, String month, String year, String category) {
        insertTransaction(type, amount, note, month, year, category, null);
    }

    // Legacy 5‑arg overload – keep for old calls, default category "Other"
    public void insertTransaction(String type, double amount, String note, String month, String year) {
        insertTransaction(type, amount, note, month, year, "Other", null);
    }

    public ArrayList<Transaction> getAllTransactions(String type) {
        ArrayList<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM transactions WHERE type=?", new String[]{type});
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndex("id"));
                String txnType = cursor.getString(cursor.getColumnIndex("type"));
                double amount = cursor.getDouble(cursor.getColumnIndex("amount"));
                String note = cursor.getString(cursor.getColumnIndex("note"));
                String month = cursor.getString(cursor.getColumnIndex("month"));
                String year = cursor.getString(cursor.getColumnIndex("year"));
                String category = cursor.getString(cursor.getColumnIndex("category"));
                String sourceType = null;
                int idxSource = cursor.getColumnIndex("source_type");
                if (idxSource != -1) {
                    sourceType = cursor.getString(idxSource);
                }
                String date = cursor.getString(cursor.getColumnIndex("date"));
                list.add(new Transaction(id, txnType, amount, note, month, year, category, date, sourceType));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /**
     * NEW: return all transactions (income + expense) ordered by date desc.
     * Used by EntryDeleteHelper to show all entries with checkboxes.
     */
    public List<Transaction> getAllTransactions() {
        List<Transaction> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM transactions ORDER BY date DESC", null);
        if (cursor.moveToFirst()) {
            do {
                long id = cursor.getLong(cursor.getColumnIndex("id"));
                String type = cursor.getString(cursor.getColumnIndex("type"));
                double amount = cursor.getDouble(cursor.getColumnIndex("amount"));
                String note = cursor.getString(cursor.getColumnIndex("note"));
                String month = cursor.getString(cursor.getColumnIndex("month"));
                String year = cursor.getString(cursor.getColumnIndex("year"));
                String category = cursor.getString(cursor.getColumnIndex("category"));
                String sourceType = null;
                int idxSource = cursor.getColumnIndex("source_type");
                if (idxSource != -1) {
                    sourceType = cursor.getString(idxSource);
                }
                String date = cursor.getString(cursor.getColumnIndex("date"));
                list.add(new Transaction(id, type, amount, note, month, year, category, date, sourceType));
            } while (cursor.moveToNext());
        }
        cursor.close();
        return list;
    }

    /**
     * NEW: delete a single transaction row by id.
     */
    public void deleteTransactionById(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("transactions", "id=?", new String[]{String.valueOf(id)});
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

    // Grouped by month, including category and source_type
    public Map<String, List<Transaction>> getTransactionsByTypeAndYearGroupedByMonth(String type, String year) {
        Map<String, List<Transaction>> map = new LinkedHashMap<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT * FROM transactions WHERE type=? AND year=? ORDER BY CAST(month AS INTEGER), date DESC",
                new String[]{type, year});
        while (cursor.moveToNext()) {
            long id = cursor.getLong(cursor.getColumnIndex("id"));
            double amount = cursor.getDouble(cursor.getColumnIndex("amount"));
            String note = cursor.getString(cursor.getColumnIndex("note"));
            String month = cursor.getString(cursor.getColumnIndex("month"));
            String txnYear = cursor.getString(cursor.getColumnIndex("year"));
            String category = cursor.getString(cursor.getColumnIndex("category"));
            String sourceType = null;
            int idxSource = cursor.getColumnIndex("source_type");
            if (idxSource != -1) {
                sourceType = cursor.getString(idxSource);
            }
            String date = cursor.getString(cursor.getColumnIndex("date"));
            Transaction txn = new Transaction(id, type, amount, note, month, txnYear, category, date, sourceType);
            if (!map.containsKey(month)) {
                map.put(month, new ArrayList<>());
            }
            map.get(month).add(txn);
        }
        cursor.close();
        return map;
    }

    public void clearAllData() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete("transactions", null, null);
    }
}
