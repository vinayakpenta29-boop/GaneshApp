package com.expensemanager;

public class Transaction {
    public String type, note, month, year, category, date;
    public double amount;

    public Transaction(String type, double amount, String note, String month, String year, String category, String date) {
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.month = month;
        this.year = year;
        this.category = category;
        this.date = date;
    }
}
