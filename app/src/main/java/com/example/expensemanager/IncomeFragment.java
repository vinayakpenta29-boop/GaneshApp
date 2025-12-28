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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    private SwipeRefreshLayout swipeRefreshLayout;

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
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);

        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(adapter);

        // Pull-to-refresh: refresh current list
        swipeRefreshLayout.setOnRefreshListener(() -> {
            selectedYear = etYear.getText().toString().trim();
            adapter.setSelectedYear(selectedYear);
            updateList();
            swipeRefreshLayout.setRefreshing(false);
        });

        // Load BC and EMI data once
        BcStore.load(requireContext());
        EmiStore.load(requireContext());

        // Three dots menu: BC/EMI menu + Delete option (Income ownerTab)
        ivMenu.setOnClickListener(v -> {
            android.widget.PopupMenu popup = new android.widget.PopupMenu(requireContext(), ivMenu);
            popup.getMenu().add(0, 1, 0, "Add BC");
            popup.getMenu().add(0, 2, 1, "View BC List");
            popup.getMenu().add(0, 3, 2, "Add EMI");
            popup.getMenu().add(0, 4, 3, "View EMI List");
            popup.getMenu().add(0, 5, 4, "Delete");

            popup.setOnMenuItemClickListener((MenuItem item) -> {
                int id = item.getItemId();
                if (id == 1) {                         // Add BC owned by INCOME tab
                    BcUiHelper.showAddBcDialog(IncomeFragment.this, "INCOME", null);
                    return true;
                } else if (id == 2) {                  // View BC List (all BC schemes)
                    BcUiHelper.showBcListDialog(IncomeFragment.this, "INCOME");
                    return true;
                } else if (id == 3) {                  // Add EMI owned by INCOME tab
                    EmiUiHelper.showAddEmiDialog(IncomeFragment.this, "INCOME", null);
                    return true;
                } else if (id == 4) {                  // View EMI List (all EMI schemes)
                    EmiUiHelper.showEmiListDialog(IncomeFragment.this, "INCOME");
                    return true;
                } else if (id == 5) {                  // Delete schemes
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
                    selectedBcId = null;
                    selectedEmiId = null;
                    return;
                }

                if ("Other".equals(cat)) {
                    showAddCategoryDialog();
                    selectedBcId = null;
                    selectedEmiId = null;
                } else if ("BC".equals(cat)) {
                    // Only Income‑owned BC schemes
                    selectedEmiId = null;
                    BcUiHelper.showSelectBcDialog(
                            IncomeFragment.this,
                            "INCOME",
                            bcId -> selectedBcId = bcId
                    );
                } else if ("EMI".equals(cat)) {
                    // Only Income‑owned EMI schemes
                    selectedBcId = null;
                    EmiUiHelper.showSelectEmiDialog(
                            IncomeFragment.this,
                            "INCOME",
                            emiId -> selectedEmiId = emiId
                    );
                } else {
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
                                String sourceType;
                                if ("Salary".equals(category)) {
                                    sourceType = "SALARY";
                                } else if ("Commission".equals(category)) {
                                    sourceType = "COMMISSION";
                                } else {
                                    sourceType = null;
                                }

                                // Option A: prefix schemeId into note for BC/EMI
                                String finalNote = note;
                                if ("BC".equals(category) && selectedBcId != null) {
                                    finalNote = selectedBcId + "||" + finalNote;
                                } else if ("EMI".equals(category) && selectedEmiId != null) {
                                    finalNote = selectedEmiId + "||" + finalNote;
                                }

                                db.insertTransaction("income", amount, finalNote, month, year, category, sourceType);

                                if ("BC".equals(category) && selectedBcId != null) {
                                    BcStore.markBcInstallmentDone(selectedBcId, null);
                                    BcStore.save(requireContext());
                                }

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

                                spinnerCategory.setSelection(0);
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
