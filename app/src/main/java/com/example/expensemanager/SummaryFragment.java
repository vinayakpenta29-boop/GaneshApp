package com.expensemanager;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.fragment.app.Fragment;

public class SummaryFragment extends Fragment {
    private DatabaseHelper db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_summary, container, false);

        db = new DatabaseHelper(getContext());
        double income = db.getTotalByType("income");
        double expenses = db.getTotalByType("expense");

        TextView tvIncome = view.findViewById(R.id.tv_income_total);
        TextView tvExpenses = view.findViewById(R.id.tv_expenses_total);
        TextView tvBalance = view.findViewById(R.id.tv_balance);

        tvIncome.setText("Total Income: " + income);
        tvExpenses.setText("Total Expenses: " + expenses);
        tvBalance.setText("Balance: " + (income - expenses));

        return view;
    }
}
