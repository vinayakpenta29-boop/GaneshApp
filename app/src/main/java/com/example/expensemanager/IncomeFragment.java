package com.expensemanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class IncomeFragment extends Fragment {
    private DatabaseHelper db;
    private TransactionAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_income, container, false);

        db = new DatabaseHelper(getContext());
        adapter = new TransactionAdapter(db.getAllTransactions("income"));

        EditText etAmount = view.findViewById(R.id.et_income_amount);
        EditText etNote = view.findViewById(R.id.et_income_note);
        Button btnAdd = view.findViewById(R.id.btn_add_income);
        RecyclerView rv = view.findViewById(R.id.rv_income);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        btnAdd.setOnClickListener(v -> {
            String amount = etAmount.getText().toString();
            String note = etNote.getText().toString();
            db.insertTransaction("income", Double.parseDouble(amount), note);
            adapter.setItems(db.getAllTransactions("income"));
        });

        return view;
    }
}
