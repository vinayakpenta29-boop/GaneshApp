package com.expensemanager;

public class Transaction {

    public long id;            // NEW: database row id
    public String type;
    public String note;
    public String month;
    public String year;
    public String category;
    public String date;
    public String sourceType;   // SALARY / COMMISSION / OTHER (can be null)
    public double amount;

    // Existing constructor (kept for backward compatibility, no id)
    public Transaction(String type,
                       double amount,
                       String note,
                       String month,
                       String year,
                       String category,
                       String date) {
        this(0L, type, amount, note, month, year, category, date, null);
    }

    // Existing constructor with sourceType (no id)
    public Transaction(String type,
                       double amount,
                       String note,
                       String month,
                       String year,
                       String category,
                       String date,
                       String sourceType) {
        this(0L, type, amount, note, month, year, category, date, sourceType);
    }

    // NEW main constructor with id + sourceType
    public Transaction(long id,
                       String type,
                       double amount,
                       String note,
                       String month,
                       String year,
                       String category,
                       String date,
                       String sourceType) {
        this.id = id;
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.month = month;
        this.year = year;
        this.category = category;
        this.date = date;
        this.sourceType = sourceType;
    }
}
