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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
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
    private String selectedBcId = null;   // which BC this expense belongs to (if any)
    private String selectedEmiId = null;  // which EMI this expense belongs to (if any)

    // New: radio buttons for Salary / Commission / Other source
    private RadioButton rbExpSalary, rbExpCommission, rbExpOther;

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
        ImageView ivMenu = view.findViewById(R.id.iv_expenses_menu);

        // New: find radio buttons
        rbExpSalary = view.findViewById(R.id.rb_exp_salary);
        rbExpCommission = view.findViewById(R.id.rb_exp_commission);
        rbExpOther = view.findViewById(R.id.rb_exp_other);
        if (rbExpOther != null) {
            rbExpOther.setChecked(true); // default to Other
        }

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
                    BcUiHelper.showBcMenu(ExpensesFragment.this, ivMenu);
                    return true;
                } else if (id == 2) {
                    // New delete flow: show BC/EMI delete dialog
                    SchemeDeleteHelper.showDeleteDialog(ExpensesFragment.this);
                    return true;
                }
                return false;
            });

            popup.show();
        });

        // Setup category spinner with "Select Category" as dummy first item
        categories = new ArrayList<>(Arrays.asList(
                "Select Category",  // index 0 = no real selection
                "EMI",
                "BC",
                "Rent",
                "Electricity Bill",
                "Ration",
                "Other"
        ));
        categoryAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, categories);
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(categoryAdapter);
        spinnerCategory.setSelection(0); // start with "Select Category"

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
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
                    // Ask which BC scheme this expense belongs to
                    selectedEmiId = null;
                    BcUiHelper.showSelectBcDialog(ExpensesFragment.this, bcId -> selectedBcId = bcId);
                } else if ("EMI".equals(cat)) {
                    // Ask which EMI scheme this expense belongs to
                    selectedBcId = null;
                    EmiUiHelper.showSelectEmiDialog(ExpensesFragment.this, emiId -> selectedEmiId = emiId);
                } else {
                    selectedBcId = null;
                    selectedEmiId = null;
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

            // New: determine which source radio is selected (for later summary logic)
            boolean isSalarySource = rbExpSalary != null && rbExpSalary.isChecked();
            boolean isCommissionSource = rbExpCommission != null && rbExpCommission.isChecked();
            // Currently these flags are not stored; they will be used when DB/schema is updated.

            new AlertDialog.Builder(getContext())
                .setTitle("Confirm Add Expense")
                .setMessage("Add this expense?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            // Insert with existing 6-arg method
                            db.insertTransaction("expense", amount, note, month, year, category);

                            // If this expense belongs to a BC scheme, mark one BC installment done
                            if ("BC".equals(category) && selectedBcId != null) {
                                BcStore.markBcInstallmentDone(selectedBcId, null);
                                BcStore.save(requireContext());
                            }

                            // If this expense belongs to an EMI scheme, mark one EMI installment done
                            if ("EMI".equals(category) && selectedEmiId != null) {
                                EmiStore.markEmiInstallmentDone(selectedEmiId, null);
                                EmiStore.save(requireContext());
                            }
                            // Later you can persist isSalarySource / isCommissionSource here
                            // once a source_type column is added to the DB.
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

                            // Optional: reset radio to Other
                            if (rbExpOther != null) {
                                rbExpOther.setChecked(true);
                            }

                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (imm != null) {
                                imm.hideSoftInputFromWindow(etAmount.getWindowToken(), 0);
                                imm.hideSoftInputFromWindow(etNote.getWindowToken(), 0);
                            }

                            // Notify SummaryFragment to refresh
                            Bundle b = new Bundle();
                            b.putBoolean("refresh", true);
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
