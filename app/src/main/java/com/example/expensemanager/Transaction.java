package com.expensemanager;

public class Transaction {
    public String type;
    public String note;
    public String month;
    public String year;
    public String category;
    public String date;
    public String sourceType;   // NEW: SALARY / COMMISSION / OTHER (can be null)
    public double amount;

    // Existing constructor (kept for backward compatibility)
    public Transaction(String type,
                       double amount,
                       String note,
                       String month,
                       String year,
                       String category,
                       String date) {
        this(type, amount, note, month, year, category, date, null);
    }

    // NEW constructor used when sourceType is known
    public Transaction(String type,
                       double amount,
                       String note,
                       String month,
                       String year,
                       String category,
                       String date,
                       String sourceType) {
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
