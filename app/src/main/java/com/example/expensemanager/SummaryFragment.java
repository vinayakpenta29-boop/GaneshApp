package com.expensemanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_summary, container, false);

        db = new DatabaseHelper(getContext());
        monthlyCardsContainer = view.findViewById(R.id.monthly_cards_container);

        Toolbar toolbar = view.findViewById(R.id.summary_toolbar);
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

        // Show "Income" by default
        updateMonthCards();
        return view;
    }

    private void updateMonthCards() {
        monthlyCardsContainer.removeAllViews();
        List<String> years = db.getAllYears();

        for (String year : years) {
            Map<String, List<Transaction>> byMonth = db.getTransactionsByTypeAndYearGroupedByMonth(currentType, year);

            for (String month : byMonth.keySet()) {
                List<Transaction> txns = byMonth.get(month);

                // Create CardView for this month
                CardView card = new CardView(getContext());
                LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                cardParams.setMargins(0, 0, 0, 32); // bottom margin between months
                card.setLayoutParams(cardParams);
                card.setRadius(32);
                card.setCardElevation(6);
                card.setUseCompatPadding(true);

                LinearLayout listLayout = new LinearLayout(getContext());
                listLayout.setOrientation(LinearLayout.VERTICAL);
                listLayout.setPadding(32, 32, 32, 32);
                listLayout.setBackgroundColor(0xFFF5F5F5); // light background

                // MONTH-YEAR HEADING
                TextView monthHeading = new TextView(getContext());
                monthHeading.setText(getMonthLabel(month) + " " + year);
                monthHeading.setTextSize(18);
                monthHeading.setTextColor(0xFF22223A);
                monthHeading.setPadding(0, 0, 0, 12);
                listLayout.addView(monthHeading);

                double total = 0;
                // List entries
                for (int i = 0; i < txns.size(); i++) {
                    Transaction txn = txns.get(i);
                    TextView entry = new TextView(getContext());
                    // Changed here: field access, not getters!
                    entry.setText("₹" + txn.amount + "   " + txn.note + "   " + txn.date);
                    entry.setTextSize(16);
                    entry.setTextColor(0xFF444444);
                    entry.setPadding(0, 8, 0, 8);
                    listLayout.addView(entry);

                    total += txn.amount;

                    // Add divider unless last entry
                    if (i < txns.size() - 1) {
                        View divider = new View(getContext());
                        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT, 1);
                        divider.setLayoutParams(dividerParams);
                        divider.setBackgroundColor(0x33000000); // semi-transparent gray
                        listLayout.addView(divider);
                    }
                }

                // Monthly total
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

    // Utility to get month abbreviation or name
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
