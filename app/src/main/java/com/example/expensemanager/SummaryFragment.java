package com.expensemanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import android.view.MenuItem;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SummaryFragment extends Fragment {
    private DatabaseHelper db;
    private LinearLayout monthlyCardsContainer;
    private String currentType = "income"; // or "expense"
    private Spinner spinnerMonth;
    private TextView tvIncome, tvExpenses, tvBalance;
    private String selectedMonth = "All";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_summary, container, false);

        db = new DatabaseHelper(getContext());
        monthlyCardsContainer = view.findViewById(R.id.monthly_cards_container);

        // Setup always-on summary box TextViews
        tvIncome = view.findViewById(R.id.tv_income_total);
        tvExpenses = view.findViewById(R.id.tv_expenses_total);
        tvBalance = view.findViewById(R.id.tv_balance);

        Toolbar toolbar = view.findViewById(R.id.summary_toolbar);
        // Add Spinner for month filter (findViewById already works if Spinner is a toolbar child)
        spinnerMonth = view.findViewById(R.id.spinner_month);

        // Month List
        final String[] monthLabels = {"All", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, monthLabels);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMonth.setAdapter(monthAdapter);

        spinnerMonth.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View v, int pos, long id) {
                selectedMonth = (pos == 0) ? "All" : String.valueOf(pos);
                updateSummaryCard();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        toolbar.inflateMenu(R.menu.menu_summary);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.action_income) {
                    currentType = "income";
                    updateMonthCards();
                    return true;
                } else if (item.getItemId() == R.id.action_expenses) {
                    currentType = "expense";
                    updateMonthCards();
                    return true;
                }
                return false;
            }
        });

        // Always show income summary card for selected month by default
        updateSummaryCard();
        // Show all income/expenses month cards (three dots logic)
        updateMonthCards();
        return view;
    }

    // Always visible summary box
    private void updateSummaryCard() {
        double income, expenses;

        if (selectedMonth.equals("All")) {
            // All months, all years
            income = db.getTotalByType("income");
            expenses = db.getTotalByType("expense");
        } else {
            // For month = selectedMonth (1 for Jan, ... 12 for Dec) **all years**
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

    // Helper function to get sum for a given month and type (across all years)
    private double getTotalForMonthType(String month, String type) {
        double total = 0;
        List<String> years = db.getAllYears();
        for (String year : years) {
            List<Transaction> txns = db.getTransactionsByTypeAndYearGroupedByMonth(type, year).get(month);
            if (txns != null) {
                for (Transaction txn : txns) {
                    total += txn.amount;
                }
            }
        }
        return total;
    }

    // Income/Expenses details cards (three dots logic - unchanged)
    private void updateMonthCards() {
        monthlyCardsContainer.removeAllViews();
        List<String> years = db.getAllYears();

        for (String year : years) {
            Map<String, List<Transaction>> byMonth = db.getTransactionsByTypeAndYearGroupedByMonth(currentType, year);

            for (String month : byMonth.keySet()) {
                List<Transaction> txns = byMonth.get(month);

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

                TextView monthHeading = new TextView(getContext());
                monthHeading.setText(getMonthLabel(month) + " " + year);
                monthHeading.setTextSize(18);
                monthHeading.setTextColor(0xFF22223A);
                monthHeading.setPadding(0, 0, 0, 12);
                listLayout.addView(monthHeading);

                double total = 0;
                for (int i = 0; i < txns.size(); i++) {
                    Transaction txn = txns.get(i);
                    TextView entry = new TextView(getContext());
                    entry.setText("₹" + txn.amount + "   " + txn.note + "   " + txn.date);
                    entry.setTextSize(16);
                    entry.setTextColor(0xFF444444);
                    entry.setPadding(0, 8, 0, 8);
                    listLayout.addView(entry);

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
                totalTv.setTextColor(0xFF22223A);
                totalTv.setPadding(0, 20, 0, 0);
                totalTv.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);
                listLayout.addView(totalTv);

                card.addView(listLayout);
                monthlyCardsContainer.addView(card);
            }
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
}
