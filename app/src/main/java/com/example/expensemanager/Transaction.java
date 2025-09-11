package com.expensemanager;

public class Transaction {
    public String type, note, date;
    public double amount;

    public Transaction(String type, double amount, String note, String date) {
        this.type = type;
        this.amount = amount;
        this.note = note;
        this.date = date;
    }
}
