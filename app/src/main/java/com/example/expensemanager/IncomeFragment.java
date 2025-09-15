package com.expensemanager;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Map;

public class IncomeFragment extends Fragment {
    private DatabaseHelper db;
    private GroupedTransactionAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_income, container, false);

        db = new DatabaseHelper(getContext());
        adapter = new GroupedTransactionAdapter();

        EditText etAmount = view.findViewById(R.id.et_income_amount);
        EditText etNote = view.findViewById(R.id.et_income_note);
        EditText etMonth = view.findViewById(R.id.et_income_month);
        EditText etYear = view.findViewById(R.id.et_income_year);
        Button btnAdd = view.findViewById(R.id.btn_add_income);
        RecyclerView rv = view.findViewById(R.id.rv_income);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        String currentYear = ""; // You may add an input for selecting/displaying current year

        btnAdd.setOnClickListener(v -> {
            String amountStr = etAmount.getText().toString().trim();
            String note = etNote.getText().toString().trim();
            String month = etMonth.getText().toString().trim();
            String year = etYear.getText().toString().trim();

            if (amountStr.isEmpty() || month.isEmpty() || year.isEmpty()) {
                Toast.makeText(getContext(), "Enter amount, month, and year", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount;
            try {
                amount = Double.parseDouble(amountStr);
            } catch (NumberFormatException e) {
                Toast.makeText(getContext(), "Invalid amount format", Toast.LENGTH_SHORT).show();
                return;
            }

            if (amount <= 0) {
                Toast.makeText(getContext(), "Amount must be greater than 0", Toast.LENGTH_SHORT).show();
                return;
            }

            new AlertDialog.Builder(getContext())
                .setTitle("Confirm Add Income")
                .setMessage("Add this income?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            db.insertTransaction("income", amount, note, month, year);
                            return null;
                        }

                        @Override
                        protected void onPostExecute(Void aVoid) {
                            Map<String, java.util.List<Transaction>> grouped = db.getTransactionsByTypeAndYearGroupedByMonth("income", year);
                            adapter.setData(grouped);
                            etAmount.setText("");
                            etNote.setText("");
                            etMonth.setText("");
                            etYear.setText("");

                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(etAmount.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(etNote.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(etMonth.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(etYear.getWindowToken(), 0);
                        }
                    }.execute();
                })
                .setNegativeButton("No", null)
                .show();
        });

        // Initial load for an example year, e.g., "2025"
        currentYear = "2025";
        Map<String, java.util.List<Transaction>> grouped = db.getTransactionsByTypeAndYearGroupedByMonth("income", currentYear);
        adapter.setData(grouped);

        return view;
    }
}
