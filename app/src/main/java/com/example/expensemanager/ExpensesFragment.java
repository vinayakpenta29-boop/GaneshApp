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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExpensesFragment extends Fragment {
    private DatabaseHelper db;
    private GroupedTransactionAdapter adapter;
    private EditText etMonth, etYear, etAmount, etNote;
    private Spinner spinnerCategory;
    private List<String> categories;
    private ArrayAdapter<String> categoryAdapter;
    private String selectedYear = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_expenses, container, false);

        db = new DatabaseHelper(getContext());
        adapter = new GroupedTransactionAdapter();

        etAmount = view.findViewById(R.id.et_expense_amount);
        etNote = view.findViewById(R.id.et_expense_note);
        etMonth = view.findViewById(R.id.et_expense_month);
        etYear = view.findViewById(R.id.et_expense_year);
        spinnerCategory = view.findViewById(R.id.spinner_expense_category);
        Button btnAdd = view.findViewById(R.id.btn_add_expense);
        RecyclerView rv = view.findViewById(R.id.rv_expenses);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        // Setup category spinner
        categories = new ArrayList<>(Arrays.asList("EMI", "BC", "Rent", "Electricity Bill", "Ration", "Other"));
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                if (categories.get(pos).equals("Other")) {
                    showAddCategoryDialog();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Month only allows 1-12 numbers
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
            String category = spinnerCategory.getSelectedItem().toString();

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
                .setTitle("Confirm Add Expense")
                .setMessage("Add this expense?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            db.insertTransaction("expense", amount, note, month, year, category);
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

                            // --- Notify SummaryFragment to refresh ---
                            Bundle b = new Bundle();
                            b.putBoolean("refresh", true);
                            if (getParentFragmentManager() != null)
                                getParentFragmentManager().setFragmentResult("refresh_summary", b);
                        }
                    }.execute();
                })
                .setNegativeButton("No", null)
                .show();
        });

        selectedYear = etYear.getText().toString().trim();
        adapter.setSelectedYear(selectedYear);
        updateList();

        return view;
    }

    private void showAddCategoryDialog() {
        EditText input = new EditText(getContext());
        new AlertDialog.Builder(getContext())
            .setTitle("Add Category")
            .setView(input)
            .setPositiveButton("OK", (d, w) -> {
                String newCat = input.getText().toString().trim();
                if (!newCat.isEmpty() && !categories.contains(newCat)) {
                    categories.add(categories.size() - 1, newCat); // Insert before "Other"
                    categoryAdapter.notifyDataSetChanged();
                    spinnerCategory.setSelection(categories.indexOf(newCat));
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void updateList() {
        String month = etMonth.getText().toString().trim();
        String year = etYear.getText().toString().trim();
        if (!month.isEmpty() && !year.isEmpty()) {
            Map<String, List<Transaction>> grouped = db.getTransactionsByTypeAndYearGroupedByMonth("expense", year);
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
