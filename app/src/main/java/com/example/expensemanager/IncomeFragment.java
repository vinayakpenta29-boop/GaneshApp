package com.expensemanager;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
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

import java.util.List;
import java.util.Map;

import com.expensemanager.DatabaseHelper;
import com.expensemanager.GroupedTransactionAdapter;
import com.expensemanager.Transaction;

public class IncomeFragment extends Fragment {
    // ... rest of your code ...
}


// ... imports ...
import android.text.InputFilter;

public class IncomeFragment extends Fragment {
    private DatabaseHelper db;
    private GroupedTransactionAdapter adapter;
    private EditText etMonth, etYear, etAmount, etNote;
    private String selectedYear = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_income, container, false);

        db = new DatabaseHelper(getContext());
        adapter = new GroupedTransactionAdapter();

        etAmount = view.findViewById(R.id.et_income_amount);
        etNote = view.findViewById(R.id.et_income_note);
        etMonth = view.findViewById(R.id.et_income_month);
        etYear = view.findViewById(R.id.et_income_year);
        Button btnAdd = view.findViewById(R.id.btn_add_income);
        RecyclerView rv = view.findViewById(R.id.rv_income);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        // Allow only valid months (1-12)
        etMonth.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(2),
                (source, start, end, dest, dstart, dend) -> {
                    String result = dest.toString().substring(0, dstart) + source + dest.toString().substring(dend);
                    if (result.isEmpty()) return null;
                    try {
                        int value = Integer.parseInt(result);
                        if (value < 1 || value > 12) return "";
                    } catch (NumberFormatException e) { return ""; }
                    return null;
                }
        });
        // Amount InputType already set to decimal in XML

        // Filter list whenever Month or Year changes
        TextWatcher filterWatcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                selectedYear = etYear.getText().toString().trim();
                adapter.setSelectedYear(selectedYear);
                updateList();
            }
        };
        etMonth.addTextChangedListener(filterWatcher);
        etYear.addTextChangedListener(filterWatcher);

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
                            selectedYear = etYear.getText().toString().trim();
                            adapter.setSelectedYear(selectedYear);
                            updateList();
                            etAmount.setText("");
                            etNote.setText("");
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.hideSoftInputFromWindow(etAmount.getWindowToken(), 0);
                            imm.hideSoftInputFromWindow(etNote.getWindowToken(), 0);
                        }
                    }.execute();
                })
                .setNegativeButton("No", null)
                .show();
        });

        // Initial load
        selectedYear = etYear.getText().toString().trim();
        adapter.setSelectedYear(selectedYear);
        updateList();

        return view;
    }

    private void updateList() {
        String month = etMonth.getText().toString().trim();
        String year = etYear.getText().toString().trim();
        if (!month.isEmpty() && !year.isEmpty()) {
            Map<String, List<Transaction>> grouped = db.getTransactionsByTypeAndYearGroupedByMonth("income", year);
            Map<String, List<Transaction>> filtered = new java.util.LinkedHashMap<>();
            if (grouped.containsKey(month)) {
                filtered.put(month, grouped.get(month));
            }
            adapter.setData(filtered);
        } else {
            adapter.setData(new java.util.LinkedHashMap<>());
        }
    }
}
