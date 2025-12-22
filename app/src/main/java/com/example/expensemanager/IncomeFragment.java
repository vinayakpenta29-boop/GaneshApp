package com.expensemanager;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MenuItem;
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
    private String selectedBcId = null;    // which BC this income belongs to (if any)
    private String selectedEmiId = null;   // which EMI this income belongs to (if any)

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

        // Load BC and EMI data once
        BcStore.load(requireContext());
        EmiStore.load(requireContext());

        // Three dots menu: existing BC/EMI menu + new Delete option
        ivMenu.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), ivMenu);
            // Existing menu item
            popup.getMenu().add(0, 1, 0, "BC / EMI Menu");
            // New delete item
            popup.getMenu().add(0, 2, 1, "Delete");

            popup.setOnMenuItemClickListener((MenuItem item) -> {
                int id = item.getItemId();
                if (id == 1) {
                    // Old behavior – keep using your existing helper
                    BcUiHelper.showBcMenu(IncomeFragment.this, ivMenu);
                    return true;
                } else if (id == 2) {
                    // New delete flow: show BC/EMI delete dialog
                    SchemeDeleteHelper.showDeleteDialog(IncomeFragment.this);
                    return true;
                }
                return false;
            });

            popup.show();
        });

        // Setup category spinner with "Select Category" as dummy first item
        categories = new ArrayList<>(Arrays.asList(
                "Select Category",  // index 0 = no real selection
                "Salary",
                "BC",
                "Rent",
                "Commission",
                "Other"
        ));
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        spinnerCategory.setSelection(0); // start with "Select Category"

        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View v, int pos, long id) {
                String cat = categories.get(pos);

                if ("Select Category".equals(cat)) {
                    // Treat as no selection
                    selectedBcId = null;
                    selectedEmiId = null;
                    return;
                }

                if ("Other".equals(cat)) {
                    showAddCategoryDialog();
                    selectedBcId = null;
                    selectedEmiId = null;
                } else if ("BC".equals(cat)) {
                    // Ask which BC scheme this income belongs to
                    selectedEmiId = null;
                    BcUiHelper.showSelectBcDialog(IncomeFragment.this, bcId -> selectedBcId = bcId);
                } else if ("EMI".equals(cat)) {
                    // Ask which EMI scheme this income belongs to
                    selectedBcId = null;
                    EmiUiHelper.showSelectEmiDialog(IncomeFragment.this, emiId -> selectedEmiId = emiId);
                } else {
                    // Normal category, no BC/EMI link
                    selectedBcId = null;
                    selectedEmiId = null;
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

            if ("Select Category".equals(category) || category.isEmpty()) {
                Toast.makeText(getContext(), "Please select a category", Toast.LENGTH_SHORT).show();
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

                                // Decide sourceType for this income
                                // Only Salary income needs SALARY tag so we can match Salary‑radio expenses.
                                String sourceType = "Salary".equals(category) ? "SALARY" : null;

                                // Use new 7‑arg insert so source_type is stored
                                db.insertTransaction("income", amount, note, month, year, category, sourceType);

                                // If this income belongs to a BC scheme, mark one BC installment done
                                if ("BC".equals(category) && selectedBcId != null) {
                                    BcStore.markBcInstallmentDone(selectedBcId, null);
                                    BcStore.save(requireContext());
                                }

                                // If this income belongs to an EMI scheme, mark one EMI installment done
                                if ("EMI".equals(category) && selectedEmiId != null) {
                                    EmiStore.markEmiInstallmentDone(selectedEmiId, null);
                                    EmiStore.save(requireContext());
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

                                // Reset category and scheme ids after each add
                                spinnerCategory.setSelection(0); // back to "Select Category"
                                selectedBcId = null;
                                selectedEmiId = null;

                                InputMethodManager imm = (InputMethodManager) getActivity()
                                        .getSystemService(Context.INPUT_METHOD_SERVICE);
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
                        // Insert before "Other"
                        int insertIndex = Math.max(categories.indexOf("Other"), 1);
                        categories.add(insertIndex, newCat);
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
