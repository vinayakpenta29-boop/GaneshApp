package com.expensemanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class ExpensesFragment extends Fragment {
    private DatabaseHelper db;
    private TransactionAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        db = new DatabaseHelper(getContext());
        adapter = new TransactionAdapter(db.getAllTransactions("expense"));

        EditText etAmount = view.findViewById(R.id.et_expense_amount);
        EditText etNote = view.findViewById(R.id.et_expense_note);
        Button btnAdd = view.findViewById(R.id.btn_add_expense);
        RecyclerView rv = view.findViewById(R.id.rv_expenses);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String note = etNote.getText().toString().trim();

            if(amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Enter a valid amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;

            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid amount format", Toast.LENGTH_SHORT).show();
                return;
            }

            if(amount <= 0) {
                Toast.makeText(getContext(), "Amount must be > 0", Toast.LENGTH_SHORT).show();
                return;
            }

            db.insertTransaction("expense", amount, note);
            adapter.setItems(db.getAllTransactions("expense"));
            etAmount.setText("");
            etNote.setText("");
        });

        return view;
    }
}
