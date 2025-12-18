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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class IncomeFragment extends Fragment {
    private DatabaseHelper db;
    private GroupedTransactionAdapter adapter;
    private EditText etMonth, etYear, etAmount, etNote;
    private Spinner spinnerCategory;
    private List<String> categories;
    private ArrayAdapter<String> categoryAdapter;
    private String selectedYear = "";
    private String selectedBcId = null;   // which BC this income belongs to (if any)

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_income, container, false);

        db = new DatabaseHelper(getContext());
        adapter = new GroupedTransactionAdapter();

        etAmount = view.findViewById(R.id.et_income_amount);
        etNote = view.findViewById(R.id.et_income_note);
        etMonth = view.findViewById(R.id.et_income_month);
        etYear = view.findViewById(R.id.et_income_year);
        spinnerCategory = view.findViewById(R.id.spinner_income_category);
        Button btnAdd = view.findViewById(R.id.btn_add_income);
        RecyclerView rv = view.findViewById(R.id.rv_income);
        ImageView ivMenu = view.findViewById(R.id.iv_income_menu);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        // Load BC data once
        BcStore.load(requireContext());

        // Three dots menu for BC (Add BC / View BC List)
        ivMenu.setOnClickListener(v -> BcUiHelper.showBcMenu(IncomeFragment.this, ivMenu));

        // Setup category spinner (same list as expenses)
        categories = new ArrayList<>(Arrays.asList("EMI", "BC", "Rent", "Electricity Bill", "Ration", "Other"));
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);

        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int pos, long id) {
                String cat = categories.get(pos);
                if ("Other".equals(cat)) {
                    showAddCategoryDialog();
                    selectedBcId = null;
                } else if ("BC".equals(cat)) {
                    // Ask which BC scheme this income belongs to
                    BcUiHelper.showSelectBcDialog(IncomeFragment.this, bcId -> {
                        selectedBcId = bcId;
                    });
                } else {
                    // Normal category, no BC link
                    selectedBcId = null;
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Allow only valid months (1-12)
        etMonth.setFilters(new InputFilter[]{
                new InputFilter.LengthFilter(2),
                (source, start, end, dest, dstart, dend) -> {
                    String result = dest.toString().substring(0, dstart) + source + dest.toString().substring(dend);
                    if (result.isEmpty()) return null;
                    try {
                        int value = Integer.parseInt(result);
                        if (value < 1 || value > 12) return "";
                    } catch (NumberFormatException e) {
                        return "";
                    }
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
            String category = spinnerCategory.getSelectedItem() != null
                    ? spinnerCategory.getSelectedItem().toString()
                    : "";

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
                                // Insert with category and BC id (db must support bcId)
                                db.insertTransaction("income", amount, note, month, year, category);

                                // If this income belongs to a BC scheme, mark one installment done
                                if ("BC".equals(category) && selectedBcId != null) {
                                    BcStore.markBcInstallmentDone(selectedBcId, null);
                                    BcStore.save(requireContext());
                                }
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
                                if (imm != null) {
                                    imm.hideSoftInputFromWindow(etAmount.getWindowToken(), 0);
                                    imm.hideSoftInputFromWindow(etNote.getWindowToken(), 0);
                                }
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

    private void showAddCategoryDialog() {
        EditText input = new EditText(getContext());
        new AlertDialog.Builder(getContext())
                .setTitle("Add Category")
                .setView(input)
                .setPositiveButton("OK", (d, w) -> {
                    String newCat = input.getText().toString().trim();
                    if (!newCat.isEmpty() && !categories.contains(newCat)) {
                        categories.add(categories.size() - 1, newCat); // before "Other"
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
            Map<String, List<Transaction>> grouped =
                    db.getTransactionsByTypeAndYearGroupedByMonth("income", year);
            Map<String, List<Transaction>> filtered = new LinkedHashMap<>();
            if (grouped.containsKey(month)) {
                filtered.put(month, grouped.get(month));
            }
            adapter.setData(filtered);
        } else {
            adapter.setData(new LinkedHashMap<>());
        }
    }
}
