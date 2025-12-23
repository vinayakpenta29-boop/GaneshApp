package com.expensemanager;

import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.*;

public class SummaryFragment extends Fragment {
    private DatabaseHelper db;
    private LinearLayout monthlyCardsContainer;
    private Spinner spinnerMonth, spinnerYear;
    private TextView tvIncome, tvExpenses, tvBalance;
    private String selectedMonth = "All";
    private String selectedYear = "All";
    private String currentType = null; // No default: only show on selection
    private String currentCategoryFilter = null;
    private FloatingActionButton btnReset;
    private CircularProgressIndicator resetProgress;
    private Handler holdHandler = new Handler();
    private final int HOLD_TIME = 5000;
    private boolean isResetHeld = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_summary, container, false);

        db = new DatabaseHelper(getContext());
        monthlyCardsContainer = view.findViewById(R.id.monthly_cards_container);
        tvIncome = view.findViewById(R.id.tv_income_total);
        tvExpenses = view.findViewById(R.id.tv_expenses_total);
        tvBalance = view.findViewById(R.id.tv_balance);
        Toolbar toolbar = view.findViewById(R.id.summary_toolbar);
        spinnerMonth = view.findViewById(R.id.spinner_month);
        spinnerYear = view.findViewById(R.id.spinner_year);
        btnReset = view.findViewById(R.id.btn_reset);
        resetProgress = view.findViewById(R.id.reset_progress);

        // Listen for refresh requests from other fragments
        getParentFragmentManager().setFragmentResultListener("refresh_summary", this, (key, bundle) -> {
            updateSummaryCard();
            updateMonthCards();
        });

        // Month spinner setup and listener
        final String[] monthLabels = {"All", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, monthLabels);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);
        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                selectedMonth = (pos == 0) ? "All" : String.valueOf(pos);
                updateSummaryCard();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Year spinner setup and listener
        List<String> yearList = new ArrayList<>();
        yearList.add("All");
        yearList.addAll(db.getAllYears());
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, yearList);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerYear.setAdapter(yearAdapter);
        spinnerYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                selectedYear = yearList.get(pos);
                updateSummaryCard();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        toolbar.inflateMenu(R.menu.menu_summary);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_income) {
                currentType = "income";
                currentCategoryFilter = null;
                updateMonthCards();
                return true;
            } else if (item.getItemId() == R.id.action_expenses) {
                currentType = "expense";
                currentCategoryFilter = null;
                updateMonthCards();
                return true;
            } else if (item.getItemId() == R.id.action_category) {
                showCategoryFilterDialog();
                return true;
            }
            return false;
        });

        // Reset FAB logic: hold 5s to trigger erase with animation
        btnReset.setOnTouchListener(new View.OnTouchListener() {
            private long startTime;
            private Runnable progressRunnable;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        isResetHeld = true;
                        startTime = System.currentTimeMillis();
                        resetProgress.setVisibility(View.VISIBLE);
                        resetProgress.setProgress(0);
                        progressRunnable = new Runnable() {
                            @Override
                            public void run() {
                                if (!isResetHeld) return;
                                long elapsed = System.currentTimeMillis() - startTime;
                                int percent = (int) Math.min((elapsed * 100) / HOLD_TIME, 100);
                                resetProgress.setProgress(percent, true);
                                if (percent < 100) {
                                    holdHandler.postDelayed(this, 16);
                                } else {
                                    isResetHeld = false;
                                    resetProgress.setVisibility(View.GONE);
                                    showPasswordDialog();
                                }
                            }
                        };
                        holdHandler.post(progressRunnable);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        isResetHeld = false;
                        resetProgress.setVisibility(View.GONE);
                        resetProgress.setProgress(0, false);
                        holdHandler.removeCallbacksAndMessages(null);
                        return true;
                }
                return false;
            }
        });

        updateSummaryCard();
        updateMonthCards();
        return view;
    }

    private void showPasswordDialog() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
        final EditText input = new EditText(getContext());
        input.setHint("Enter Password");
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        dialogBuilder.setTitle("Confirm Reset")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("OK", (dialog, which) -> {
                String pass = input.getText().toString();
                if ("1234".equals(pass)) {
                    db.clearAllData();
                    Toast.makeText(getContext(), "All data erased", Toast.LENGTH_SHORT).show();
                    updateSummaryCard();
                    updateMonthCards();
                } else {
                    Toast.makeText(getContext(), "Incorrect password", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
            .show();
    }

    private void showCategoryFilterDialog() {
        if (currentType == null) {
            Toast.makeText(getContext(), "Select Income or Expenses first", Toast.LENGTH_SHORT).show();
            return;
        }
        ArrayList<Transaction> all = db.getAllTransactions(currentType);
        Set<String> categories = new LinkedHashSet<>();
        for (Transaction t : all) {
            if (t.category != null && !t.category.trim().isEmpty()) categories.add(t.category);
        }
        final List<String> categoryList = new ArrayList<>(categories);
        if (categoryList.isEmpty()) {
            Toast.makeText(getContext(), "No categories found.", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] categoryArr = categoryList.toArray(new String[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Select Category");
        builder.setItems(categoryArr, (dialog, which) -> {
            currentCategoryFilter = categoryList.get(which);
            updateCategoryGroup();
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void updateSummaryCard() {
        double income = 0, expenses = 0;
        if (selectedYear.equals("All") && selectedMonth.equals("All")) {
            income = db.getTotalByType("income");
            expenses = db.getTotalByType("expense");
        } else if (!selectedYear.equals("All") && selectedMonth.equals("All")) {
            income = getTotalForYear(selectedYear, "income");
            expenses = getTotalForYear(selectedYear, "expense");
        } else if (!selectedYear.equals("All") && !selectedMonth.equals("All")) {
            income = getTotalForYearMonth(selectedYear, selectedMonth, "income");
            expenses = getTotalForYearMonth(selectedYear, selectedMonth, "expense");
        } else if (selectedYear.equals("All") && !selectedMonth.equals("All")) {
            income = getTotalForMonthType(selectedMonth, "income");
            expenses = getTotalForMonthType(selectedMonth, "expense");
        }
        double balance = income - expenses;
        tvIncome.setText("Total Income: " + String.format(Locale.US, "%.2f", income));
        tvExpenses.setText("Total Expenses: " + String.format(Locale.US, "%.2f", expenses));
        tvBalance.setText("Balance: " + String.format(Locale.US, "%.2f", balance));
        tvBalance.setTextColor(0xFFFFB300); // Orange
        tvBalance.setTypeface(tvBalance.getTypeface(), android.graphics.Typeface.BOLD);
    }

    private double getTotalForYear(String year, String type) {
        double total = 0;
        Map<String, List<Transaction>> byMonth = db.getTransactionsByTypeAndYearGroupedByMonth(type, year);
        if (byMonth != null) {
            for (List<Transaction> txns : byMonth.values()) {
                for (Transaction txn : txns) total += txn.amount;
            }
        }
        return total;
    }
    private double getTotalForYearMonth(String year, String month, String type) {
        double total = 0;
        Map<String, List<Transaction>> byMonth = db.getTransactionsByTypeAndYearGroupedByMonth(type, year);
        List<Transaction> txns = byMonth != null ? byMonth.get(month) : null;
        if (txns != null) for (Transaction txn : txns) total += txn.amount;
        return total;
    }
    private double getTotalForMonthType(String month, String type) {
        double total = 0;
        List<String> years = db.getAllYears();
        for (String year : years) {
            List<Transaction> txns = db.getTransactionsByTypeAndYearGroupedByMonth(type, year).get(month);
            if (txns != null) {
                for (Transaction txn : txns) total += txn.amount;
            }
        }
        return total;
    }

    private void updateMonthCards() {
        monthlyCardsContainer.removeAllViews();

        if (currentType == null) {
            // Don't show any Income/Expenses entry cards until picked by user
            return;
        }

        currentCategoryFilter = null;
        TextView heading = new TextView(getContext());
        heading.setText(currentType.equals("income") ? "Income" : "Expenses");
        heading.setTextColor(currentType.equals("income") ? 0xFF388E3C : 0xFFF44336);
        heading.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        heading.setTextSize(22);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headingParams.setMargins(0, 20, 0, 18);
        monthlyCardsContainer.addView(heading, headingParams);

        List<String> years = db.getAllYears();
        for (String year : years) {
            Map<String, List<Transaction>> byMonth = db.getTransactionsByTypeAndYearGroupedByMonth(currentType, year);
            for (String month : byMonth.keySet()) {
                List<Transaction> txns = byMonth.get(month);
                if (txns == null || txns.isEmpty()) continue;
                CardView card = new CardView(getContext());
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(0, 0, 0, 32);
                card.setLayoutParams(cardParams);
                card.setRadius(32);
                card.setCardElevation(6);
                card.setUseCompatPadding(true);

                LinearLayout listLayout = new LinearLayout(getContext());
                listLayout.setOrientation(LinearLayout.VERTICAL);
                listLayout.setPadding(32, 32, 32, 32);
                listLayout.setBackgroundColor(0xFFF5F5F5);

                // MONTH-YEAR (centered, bold)
                TextView monthHeading = new TextView(getContext());
                monthHeading.setText(getMonthLabel(month) + " " + year);
                monthHeading.setTextSize(16);
                monthHeading.setTextColor(0xFF22223A);
                monthHeading.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                monthHeading.setTypeface(null, android.graphics.Typeface.BOLD);
                monthHeading.setPadding(0, 0, 0, 12);
                listLayout.addView(monthHeading);

                double total = 0;
                for (int i = 0; i < txns.size(); i++) {
                    Transaction txn = txns.get(i);

                    LinearLayout entryRow = new LinearLayout(getContext());
                    entryRow.setOrientation(LinearLayout.HORIZONTAL);
                    entryRow.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    entryRow.setPadding(0, 8, 0, 8);

                    // Amount (bold)
                    TextView amountView = new TextView(getContext());
                    amountView.setText("₹" + txn.amount);
                    amountView.setTypeface(null, android.graphics.Typeface.BOLD);
                    amountView.setTextColor(0xFF444444);
                    amountView.setTextSize(14);
                    amountView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));

                    // Note (center)
                    TextView noteView = new TextView(getContext());
                    noteView.setText(txn.note);
                    noteView.setTextColor(0xFF444444);
                    noteView.setTextSize(14);
                    noteView.setSingleLine(true);
                    noteView.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
                    noteView.setMarqueeRepeatLimit(-1);
                    noteView.setSelected(true);
                    noteView.setHorizontalFadingEdgeEnabled(true);
                    noteView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f));

                    // Date (DD-MM-YY)
                    TextView dateView = new TextView(getContext());
                    dateView.setText(formatDate(txn.date));
                    dateView.setTextColor(0xFF888888);
                    dateView.setTextSize(14);
                    dateView.setGravity(Gravity.END);
                    dateView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));

                    entryRow.addView(amountView);
                    entryRow.addView(noteView);
                    entryRow.addView(dateView);

                    listLayout.addView(entryRow);
                    total += txn.amount;

                    if (i < txns.size() - 1) {
                        View divider = new View(getContext());
                        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 1);
                        divider.setLayoutParams(dividerParams);
                        divider.setBackgroundColor(0x33000000);
                        listLayout.addView(divider);
                    }
                }
                TextView totalTv = new TextView(getContext());
                totalTv.setText(String.format(Locale.US, "Total: ₹%.2f", total));
                totalTv.setTextSize(16);
                totalTv.setTextColor(0xFFFFB300);
                totalTv.setTypeface(null, android.graphics.Typeface.BOLD);
                totalTv.setPadding(0, 20, 0, 0);
                totalTv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                listLayout.addView(totalTv);
                card.addView(listLayout);
                monthlyCardsContainer.addView(card);
            }
        }
    }

    // Show entries grouped and filtered by category
    private void updateCategoryGroup() {
        if (currentCategoryFilter == null) {
            updateMonthCards();
            return;
        }

        monthlyCardsContainer.removeAllViews();

        // Category Header
        TextView heading = new TextView(getContext());
        heading.setText(currentCategoryFilter);
        heading.setTextColor(0xFF262651);
        heading.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        heading.setTextSize(22);
        heading.setTypeface(null, android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams headingParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        headingParams.setMargins(0, 20, 0, 18);
        monthlyCardsContainer.addView(heading, headingParams);

        // Get all transactions by month+year, filtered by selected category+type
        List<String> years = db.getAllYears();
        Map<String, Map<String, List<Transaction>>> categoryGrouped = new LinkedHashMap<>();
        for (String year : years) {
            Map<String, List<Transaction>> byMonth = db.getTransactionsByTypeAndYearGroupedByMonth(currentType, year);
            for (String month : byMonth.keySet()) {
                List<Transaction> txnsForMonth = byMonth.get(month);
                for (Transaction txn : txnsForMonth) {
                    if (txn.category != null && txn.category.equals(currentCategoryFilter)) {
                        if (!categoryGrouped.containsKey(year))
                            categoryGrouped.put(year, new LinkedHashMap<>());
                        if (!categoryGrouped.get(year).containsKey(month))
                            categoryGrouped.get(year).put(month, new ArrayList<>());
                        categoryGrouped.get(year).get(month).add(txn);
                    }
                }
            }
        }

        double allTotal = 0;

        for (String year : categoryGrouped.keySet()) {
            Map<String, List<Transaction>> monthsMap = categoryGrouped.get(year);
            for (String month : monthsMap.keySet()) {
                List<Transaction> txns = monthsMap.get(month);
                if (txns == null || txns.isEmpty()) continue;

                CardView card = new CardView(getContext());
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(0, 0, 0, 32);
                card.setLayoutParams(cardParams);
                card.setRadius(32);
                card.setCardElevation(6);
                card.setUseCompatPadding(true);

                LinearLayout listLayout = new LinearLayout(getContext());
                listLayout.setOrientation(LinearLayout.VERTICAL);
                listLayout.setPadding(32, 32, 32, 32);
                listLayout.setBackgroundColor(0xFFF5F5F5);

                // Month-Year header
                TextView monthHeading = new TextView(getContext());
                monthHeading.setText(getMonthLabel(month) + " " + year);
                monthHeading.setTextSize(16);
                monthHeading.setTextColor(0xFF22223A);
                monthHeading.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                monthHeading.setTypeface(null, android.graphics.Typeface.BOLD);
                monthHeading.setPadding(0, 0, 0, 12);
                listLayout.addView(monthHeading);

                double monthlyTotal = 0;
                for (int i = 0; i < txns.size(); i++) {
                    Transaction txn = txns.get(i);

                    LinearLayout entryRow = new LinearLayout(getContext());
                    entryRow.setOrientation(LinearLayout.HORIZONTAL);
                    entryRow.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    entryRow.setPadding(0, 8, 0, 8);

                    // Amount
                    TextView amountView = new TextView(getContext());
                    amountView.setText("₹" + txn.amount);
                    amountView.setTypeface(null, android.graphics.Typeface.BOLD);
                    amountView.setTextColor(0xFF444444);
                    amountView.setTextSize(14);
                    amountView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));

                    // Note
                    TextView noteView = new TextView(getContext());
                    noteView.setText(txn.note);
                    noteView.setTextColor(0xFF444444);
                    noteView.setTextSize(14);
                    noteView.setSingleLine(true);
                    noteView.setEllipsize(android.text.TextUtils.TruncateAt.MARQUEE);
                    noteView.setMarqueeRepeatLimit(-1);
                    noteView.setSelected(true);
                    noteView.setHorizontalFadingEdgeEnabled(true);
                    noteView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.4f));

                    // Date (DD-MM-YY)
                    TextView dateView = new TextView(getContext());
                    dateView.setText(formatDate(txn.date));
                    dateView.setTextColor(0xFF888888);
                    dateView.setTextSize(14);
                    dateView.setGravity(Gravity.END);
                    dateView.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.2f));

                    entryRow.addView(amountView);
                    entryRow.addView(noteView);
                    entryRow.addView(dateView);

                    listLayout.addView(entryRow);
                    monthlyTotal += txn.amount;

                    if (i < txns.size() - 1) {
                        View divider = new View(getContext());
                        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 1);
                        divider.setLayoutParams(dividerParams);
                        divider.setBackgroundColor(0x33000000);
                        listLayout.addView(divider);
                    }
                }

                // Yellow Total line
                TextView totalTv = new TextView(getContext());
                totalTv.setText(String.format(Locale.US, "Total: ₹%.2f", monthlyTotal));
                totalTv.setTextSize(16);
                totalTv.setTextColor(0xFFFFB300);
                totalTv.setTypeface(null, android.graphics.Typeface.BOLD);
                totalTv.setPadding(0, 16, 0, 0);
                totalTv.setGravity(Gravity.CENTER_HORIZONTAL);
                listLayout.addView(totalTv);

                // For Salary category show Balance AFTER Salary‑source expenses
                if ("Salary".equals(currentCategoryFilter)) {
                    double incomeSalary = getIncomeTotalForCategoryMonthYear("Salary", month, year);
                    double expenseFromSalarySource = getExpenseTotalFromSourceForMonthYear("SALARY", month, year);
                    double salaryBalance = incomeSalary - expenseFromSalarySource;

                    TextView balanceTv = new TextView(getContext());
                    balanceTv.setText(String.format(Locale.US, "Balance: ₹%.2f", salaryBalance));
                    balanceTv.setTextSize(15);
                    balanceTv.setTextColor(0xFFFFFFFF); // green
                    balanceTv.setTypeface(null, android.graphics.Typeface.BOLD);
                    balanceTv.setPadding(0, 8, 0, 0);
                    balanceTv.setBackgroundResource(R.drawable.bg_balance_chip);
                    balanceTv.setGravity(Gravity.CENTER_HORIZONTAL);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.gravity = Gravity.CENTER_HORIZONTAL;
                    lp.topMargin = 8;
                    balanceTv.setLayoutParams(lp);
                    listLayout.addView(balanceTv);
                }
                // For Commission category show Balance AFTER Commission‑source expenses
                else if ("Commission".equals(currentCategoryFilter)) {
                    double incomeCommission = getIncomeTotalForCategoryMonthYear("Commission", month, year);
                    double expenseFromCommissionSource = getExpenseTotalFromSourceForMonthYear("COMMISSION", month, year);
                    double commissionBalance = incomeCommission - expenseFromCommissionSource;

                    TextView balanceTv = new TextView(getContext());
                    balanceTv.setText(String.format(Locale.US, "Balance: ₹%.2f", commissionBalance));
                    balanceTv.setTextSize(15);
                    balanceTv.setTextColor(0xFFFFFFFF); // green
                    balanceTv.setTypeface(null, android.graphics.Typeface.BOLD);
                    balanceTv.setPadding(24, 8, 24, 0);
                    balanceTv.setBackgroundResource(R.drawable.bg_balance_chip);
                    balanceTv.setGravity(Gravity.CENTER_HORIZONTAL);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    lp.gravity = Gravity.CENTER_HORIZONTAL;
                    lp.topMargin = 8;
                    balanceTv.setLayoutParams(lp);
                    listLayout.addView(balanceTv);
                }

                card.addView(listLayout);
                monthlyCardsContainer.addView(card);

                allTotal += monthlyTotal;
            }
        }

        if (!categoryGrouped.isEmpty()) {
            TextView totalsHeading = new TextView(getContext());
            totalsHeading.setText(String.format(Locale.US, "Total (%s): ₹%.2f", currentCategoryFilter, allTotal));
            totalsHeading.setTextColor(0xFF388E3C);
            totalsHeading.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            totalsHeading.setTextSize(17);
            totalsHeading.setTypeface(null, android.graphics.Typeface.BOLD);
            LinearLayout.LayoutParams totalParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            totalParams.setMargins(0, 0, 0, 26);
            monthlyCardsContainer.addView(totalsHeading, totalParams);
        }
    }

    private String getMonthLabel(String monthNumber) {
        try {
            int month = Integer.parseInt(monthNumber);
            String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
            if (month >= 1 && month <= 12) return months[month - 1];
        } catch (Exception ignored) {}
        return "Month";
    }

    private String formatDate(String dateRaw) {
        try {
            String[] parts = dateRaw.split(" ");
            String date = parts[0];
            String[] ymd = date.split("-");
            if (ymd.length == 3) {
                String yy = ymd[0].substring(2);
                return ymd[2] + "-" + ymd[1] + "-" + yy;
            }
        } catch (Exception e) {}
        return dateRaw;
    }

    // Helpers for per‑category totals
    public double getIncomeTotalForCategoryMonthYear(String category, String month, String year) {
        double total = 0;
        Map<String, List<Transaction>> byMonth = db.getTransactionsByTypeAndYearGroupedByMonth("income", year);
        if (byMonth == null) return 0;
        List<Transaction> txns = byMonth.get(month);
        if (txns == null) return 0;
        for (Transaction t : txns) {
            if (category.equals(t.category)) total += t.amount;
        }
        return total;
    }

    // New: expenses summed by source_type (radio), not by category
    private double getExpenseTotalFromSourceForMonthYear(String sourceType, String month, String year) {
        double total = 0;
        Map<String, List<Transaction>> byMonth = db.getTransactionsByTypeAndYearGroupedByMonth("expense", year);
        if (byMonth == null) return 0;
        List<Transaction> txns = byMonth.get(month);
        if (txns == null) return 0;
        for (Transaction t : txns) {
            if (sourceType.equals(t.sourceType)) total += t.amount;
        }
        return total;
    }

    public double getExpenseTotalForCategoryMonthYear(String category, String month, String year) {
        double total = 0;
        Map<String, List<Transaction>> byMonth = db.getTransactionsByTypeAndYearGroupedByMonth("expense", year);
        if (byMonth == null) return 0;
        List<Transaction> txns = byMonth.get(month);
        if (txns == null) return 0;
        for (Transaction t : txns) {
            if (category.equals(t.category)) total += t.amount;
        }
        return total;
    }

    public double getBalanceForCategoryMonthYear(String category, String month, String year) {
        double inc = getIncomeTotalForCategoryMonthYear(category, month, year);
        double exp = getExpenseTotalForCategoryMonthYear(category, month, year);
        return inc - exp;
    }
}
